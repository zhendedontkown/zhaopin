package com.bishe.recruitment.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.entity.ApplicationStatusLog;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobFavorite;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.ResumeEducation;
import com.bishe.recruitment.entity.ResumeSkill;
import com.bishe.recruitment.entity.SkillDict;
import com.bishe.recruitment.entity.SysRole;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.enums.UserRole;
import com.bishe.recruitment.mapper.ApplicationStatusLogMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobFavoriteMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.ResumeEducationMapper;
import com.bishe.recruitment.mapper.ResumeMapper;
import com.bishe.recruitment.mapper.ResumeSkillMapper;
import com.bishe.recruitment.mapper.SkillDictMapper;
import com.bishe.recruitment.mapper.SysRoleMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import com.bishe.recruitment.service.UserSupportService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_TEMPLATE = "classic";

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeEducationMapper resumeEducationMapper;
    private final ResumeSkillMapper resumeSkillMapper;
    private final SkillDictMapper skillDictMapper;
    private final ApplicationStatusLogMapper applicationStatusLogMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final JobFavoriteMapper jobFavoriteMapper;
    private final JobPostMapper jobPostMapper;
    private final UserSupportService userSupportService;

    public DataSeeder(SysRoleMapper sysRoleMapper, SysUserMapper sysUserMapper, CompanyProfileMapper companyProfileMapper,
                      JobseekerProfileMapper jobseekerProfileMapper, ResumeMapper resumeMapper,
                      ResumeEducationMapper resumeEducationMapper, ResumeSkillMapper resumeSkillMapper,
                      SkillDictMapper skillDictMapper, ApplicationStatusLogMapper applicationStatusLogMapper,
                      JobApplicationMapper jobApplicationMapper, JobFavoriteMapper jobFavoriteMapper, JobPostMapper jobPostMapper,
                      UserSupportService userSupportService) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserMapper = sysUserMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.resumeMapper = resumeMapper;
        this.resumeEducationMapper = resumeEducationMapper;
        this.resumeSkillMapper = resumeSkillMapper;
        this.skillDictMapper = skillDictMapper;
        this.applicationStatusLogMapper = applicationStatusLogMapper;
        this.jobApplicationMapper = jobApplicationMapper;
        this.jobFavoriteMapper = jobFavoriteMapper;
        this.jobPostMapper = jobPostMapper;
        this.userSupportService = userSupportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        seedSkillDict();
        syncAllUserPasswords();
        seedAdmin();
        seedDemoData();
    }

    private void syncAllUserPasswords() {
        String encodedPassword = userSupportService.passwordEncoder().encode(DEFAULT_PASSWORD);
        List<SysUser> users = sysUserMapper.selectList(null);
        for (SysUser user : users) {
            user.setPassword(encodedPassword);
            sysUserMapper.updateById(user);
        }
    }

    private void seedRoles() {
        for (UserRole role : UserRole.values()) {
            if (sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role.name())) == 0) {
                SysRole sysRole = new SysRole();
                sysRole.setCode(role.name());
                sysRole.setName(role.name());
                sysRoleMapper.insert(sysRole);
            }
        }
    }

    private void seedSkillDict() {
        List<String> skills = List.of(
                "Java", "Spring Boot", "MySQL", "MyBatis Plus", "Vue3", "TypeScript", "Element Plus", "Redis",
                "Linux", "Docker", "Python", "SQL", "Figma", "用户研究", "产品设计", "数据分析", "BI", "数据治理",
                "自动化测试", "AI测试", "Prompt 评测", "DevOps", "Kubernetes", "Golang", "C++", "云原生",
                "电商运营", "数据运营", "新媒体运营", "渠道运营", "招聘运营", "人力资源", "实施交付", "技术支持",
                "稳定性治理", "A/B 测试", "埋点分析", "CRM", "医疗信息化", "质量管理");
        for (String skill : skills) {
            ensureSkillDict(skill);
        }
    }

    private void seedAdmin() {
        SysUser admin = findUserByEmail("admin@recruitment.local");
        if (admin == null) {
            userSupportService.createUser("admin@recruitment.local", "13900000000", DEFAULT_PASSWORD, "系统管理员", UserRole.ADMIN);
            return;
        }
        admin.setUsername("admin@recruitment.local");
        admin.setEmail("admin@recruitment.local");
        admin.setPhone("13900000000");
        admin.setDisplayName("系统管理员");
        admin.setStatus("ACTIVE");
        sysUserMapper.updateById(admin);
        ensureUserRole(admin.getId(), UserRole.ADMIN);
    }

    private void seedDemoData() {
        cleanupObviousBadJobs();

        for (CompanySeed seed : companySeeds()) {
            SysUser user = ensureUser(seed.email(), seed.phone(), seed.displayName(), UserRole.COMPANY);
            upsertCompanyProfile(user, seed);
        }

        for (JobseekerSeed seed : jobseekerSeeds()) {
            SysUser user = ensureUser(seed.email(), seed.phone(), seed.fullName(), UserRole.JOBSEEKER);
            upsertJobseekerProfile(user, seed);
            upsertResume(user, seed);
        }

        int index = 0;
        for (JobSeed seed : jobSeeds()) {
            SysUser companyUser = findUserByEmail(seed.companyEmail());
            if (companyUser != null) {
                upsertJobPost(companyUser, seed, index++);
            }
        }
    }

    private void cleanupObviousBadJobs() {
        List<JobPost> badJobs = jobPostMapper.selectList(null).stream()
                .filter(this::isObviousBadJob)
                .toList();
        if (badJobs.isEmpty()) {
            return;
        }

        List<Long> badJobIds = badJobs.stream().map(JobPost::getId).toList();
        List<Long> badApplicationIds = jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                        .in(JobApplication::getJobId, badJobIds))
                .stream()
                .map(JobApplication::getId)
                .toList();

        if (!badApplicationIds.isEmpty()) {
            applicationStatusLogMapper.delete(new LambdaQueryWrapper<ApplicationStatusLog>()
                    .in(ApplicationStatusLog::getApplicationId, badApplicationIds));
        }
        jobFavoriteMapper.delete(new LambdaQueryWrapper<JobFavorite>().in(JobFavorite::getJobId, badJobIds));
        jobApplicationMapper.delete(new LambdaQueryWrapper<JobApplication>().in(JobApplication::getJobId, badJobIds));
        jobPostMapper.delete(new LambdaQueryWrapper<JobPost>().in(JobPost::getId, badJobIds));
    }

    private boolean isObviousBadJob(JobPost job) {
        String title = safe(job.getTitle()).trim().toLowerCase();
        String category = safe(job.getCategory()).trim().toLowerCase();
        String location = safe(job.getLocation()).trim().toLowerCase();
        String experienceRequirement = safe(job.getExperienceRequirement()).trim().toLowerCase();
        String educationRequirement = safe(job.getEducationRequirement()).trim().toLowerCase();
        String description = safe(job.getDescription()).toLowerCase();

        return title.equals("dada")
                || category.equals("31231")
                || location.equals("asd asd a")
                || experienceRequirement.equals("e")
                || educationRequirement.equals("benke")
                || containsNoise(description);
    }

    private boolean containsNoise(String text) {
        return text.contains("dasdasdasdasda")
                || text.contains("suiodgfh")
                || text.contains("qawu87")
                || text.contains("tasfd87")
                || text.contains("awseyhrf");
    }

    private SysUser ensureUser(String email, String phone, String displayName, UserRole role) {
        SysUser user = findUserByEmail(email);
        if (user == null) {
            return userSupportService.createUser(email, phone, DEFAULT_PASSWORD, displayName, role);
        }
        user.setUsername(email);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDisplayName(displayName);
        user.setStatus("ACTIVE");
        sysUserMapper.updateById(user);
        ensureUserRole(user.getId(), role);
        return user;
    }

    private void ensureUserRole(Long userId, UserRole role) {
        if (!userSupportService.getRoles(userId).contains(role.name())) {
            userSupportService.assignRole(userId, role);
        }
    }

    private void upsertCompanyProfile(SysUser user, CompanySeed seed) {
        CompanyProfile profile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, user.getId()));
        if (profile == null) {
            profile = new CompanyProfile();
            profile.setUserId(user.getId());
        }
        profile.setCompanyName(seed.companyName());
        profile.setUnifiedSocialCreditCode(seed.unifiedSocialCreditCode());
        profile.setContactPerson(seed.contactPerson());
        profile.setPhone(seed.phone());
        profile.setEmail(seed.email());
        profile.setAddress(seed.address());
        profile.setDescription(seed.description());
        profile.setAuditStatus(CompanyAuditStatus.APPROVED.name());
        if (profile.getId() == null) {
            companyProfileMapper.insert(profile);
        } else {
            companyProfileMapper.updateById(profile);
        }
    }

    private void upsertJobseekerProfile(SysUser user, JobseekerSeed seed) {
        JobseekerProfile profile = jobseekerProfileMapper.selectOne(
                new LambdaQueryWrapper<JobseekerProfile>().eq(JobseekerProfile::getUserId, user.getId()));
        if (profile == null) {
            profile = new JobseekerProfile();
            profile.setUserId(user.getId());
        }
        profile.setFullName(seed.fullName());
        profile.setPhone(seed.phone());
        profile.setEmail(seed.email());
        profile.setDesiredPositionCategory(seed.desiredPositionCategory());
        profile.setPreferredCity(seed.preferredCity());
        profile.setExpectedSalaryMin(seed.expectedSalaryMin());
        profile.setExpectedSalaryMax(seed.expectedSalaryMax());
        profile.setHighestEducation(seed.highestEducation());
        profile.setYearsOfExperience(seed.yearsOfExperience());
        profile.setPreferredSkillTagsJson(toJsonArray(seed.skills()));
        profile.setPreferredBenefitTagsJson(toJsonArray(List.of("五险一金", "带薪年假")));
        if (profile.getId() == null) {
            jobseekerProfileMapper.insert(profile);
        } else {
            jobseekerProfileMapper.updateById(profile);
        }
    }

    private void upsertResume(SysUser user, JobseekerSeed seed) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, user.getId()));
        if (resume == null) {
            resume = new Resume();
            resume.setUserId(user.getId());
        }
        resume.setTemplateCode(DEFAULT_TEMPLATE);
        resume.setFullName(seed.fullName());
        resume.setGender(seed.gender());
        resume.setAge(seed.age());
        resume.setBirthDate(LocalDate.of(LocalDate.now().getYear() - seed.age(), 6, 15));
        resume.setDisplayAge(Boolean.TRUE);
        resume.setPhone(seed.phone());
        resume.setEmail(seed.email());
        resume.setCity(seed.preferredCity());
        resume.setSummary(seed.summary());
        resume.setExpectedCategory(seed.desiredPositionCategory());
        resume.setExpectedSalaryMin(seed.expectedSalaryMin());
        resume.setExpectedSalaryMax(seed.expectedSalaryMax());
        resume.setHighestEducation(seed.highestEducation());
        resume.setYearsOfExperience(seed.yearsOfExperience());
        resume.setCompletenessScore(96);
        if (resume.getId() == null) {
            resumeMapper.insert(resume);
        } else {
            resumeMapper.updateById(resume);
        }

        resumeEducationMapper.delete(new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resume.getId()));
        ResumeEducation education = new ResumeEducation();
        education.setResumeId(resume.getId());
        education.setSchoolName(seed.schoolName());
        education.setMajor(seed.major());
        education.setDegree(seed.highestEducation());
        education.setStartDate(seed.educationStartDate());
        education.setEndDate(seed.educationEndDate());
        education.setCurrentFlag(Boolean.FALSE);
        education.setDescription(seed.educationDescription());
        education.setSortOrder(0);
        resumeEducationMapper.insert(education);

        resumeSkillMapper.delete(new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resume.getId()));
        for (String skillName : seed.skills()) {
            SkillDict dict = ensureSkillDict(skillName);
            ResumeSkill resumeSkill = new ResumeSkill();
            resumeSkill.setResumeId(resume.getId());
            resumeSkill.setSkillId(dict == null ? null : dict.getId());
            resumeSkill.setSkillName(skillName);
            resumeSkillMapper.insert(resumeSkill);
        }
    }

    private void upsertJobPost(SysUser companyUser, JobSeed seed, int index) {
        JobPost jobPost = jobPostMapper.selectOne(
                new LambdaQueryWrapper<JobPost>().eq(JobPost::getJobCode, seed.jobCode()));
        if (jobPost == null) {
            jobPost = new JobPost();
        }
        jobPost.setCompanyUserId(companyUser.getId());
        jobPost.setJobCode(seed.jobCode());
        jobPost.setTitle(seed.title());
        jobPost.setCategory(seed.category());
        jobPost.setLocation(seed.location());
        jobPost.setSalaryMin(seed.salaryMin());
        jobPost.setSalaryMax(seed.salaryMax());
        jobPost.setExperienceRequirement(seed.experienceRequirement());
        jobPost.setEducationRequirement(seed.educationRequirement());
        jobPost.setHeadcount(seed.headcount());
        jobPost.setDescription(buildJobDescription(seed));
        jobPost.setBenefitTagsJson(toJsonArray(seed.benefits()));
        jobPost.setSkillTagsJson(toJsonArray(List.of(seed.requiredSkill(), seed.bonusSkill())));
        jobPost.setStatus(JobStatus.PUBLISHED.name());
        jobPost.setPublishedAt(LocalDateTime.of(2026, 4, (index % 6) + 1, 9 + (index % 4), 0));
        jobPost.setExpireAt(LocalDateTime.of(2026, 6, 30, 23, 59));
        if (jobPost.getId() == null) {
            jobPostMapper.insert(jobPost);
        } else {
            jobPostMapper.updateById(jobPost);
        }
    }

    private String buildJobDescription(JobSeed seed) {
        return """
                职位描述
                岗位职责:
                1.负责%s相关工作的规划、拆解与推进，围绕%s持续输出可落地方案，确保需求从评审、开发到验收形成完整闭环，并能够结合业务优先级合理安排交付节奏。
                2.承担%s，能够主动梳理关键链路、识别潜在风险，推动文档沉淀、过程复盘与质量改进，让团队在多项目并行情况下依然保持稳定交付。
                3.与产品、设计、测试、实施、运营等角色保持高频协同，围绕%s推进联调、反馈处理、上线支持和用户问题闭环，及时同步进度并推动跨部门事项落地。
                4.持续关注业务数据、用户反馈和流程瓶颈，在%s方向沉淀标准化方法、模板和经验，帮助团队提升效率、降低沟通成本并优化最终用户体验。
                5.关注 AI 工具、自动化能力和行业最佳实践，在合适场景下推动新方案验证与工具落地，提升岗位工作的深度、广度和可复制性。
                任职要求:
                1.本科及以上学历，具备%s相关工作经验，能够独立承担业务模块或专项工作，具备较强的责任心和结果意识。
                2.熟悉%s，具有扎实的专业基础和良好的结构化表达能力，能够清晰输出文档、方案、评审意见和复盘结论。
                3.具备优秀的沟通协作能力、问题分析能力和执行推进能力，能够在压力场景下保持节奏，主动暴露问题并推动资源协同。
                4.对%s有兴趣或实践经验者优先，愿意持续学习新技术、新工具和新业务场景，能够适应快速迭代的团队协作方式。
                """.formatted(
                seed.businessContext(),
                seed.title(),
                seed.coreDelivery(),
                seed.collaborationScene(),
                seed.category(),
                seed.experienceRequirement(),
                seed.requiredSkill(),
                seed.bonusSkill()
        );
    }

    private SkillDict ensureSkillDict(String skillName) {
        SkillDict dict = skillDictMapper.selectOne(
                new LambdaQueryWrapper<SkillDict>().eq(SkillDict::getSkillName, skillName));
        if (dict != null) {
            return dict;
        }
        dict = new SkillDict();
        dict.setSkillName(skillName);
        dict.setCategory("默认");
        skillDictMapper.insert(dict);
        return dict;
    }

    private SysUser findUserByEmail(String email) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
    }

    private String toJsonArray(List<String> items) {
        return items.stream().map(this::quoteJson).collect(Collectors.joining(",", "[", "]"));
    }

    private String quoteJson(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<CompanySeed> companySeeds() {
        return List.of(
                new CompanySeed(
                        "hr@futuretech.com",
                        "13800000001",
                        "未来矩阵招聘经理",
                        "未来矩阵科技有限公司",
                        "91310000MA1K123456",
                        "张思远",
                        "深圳市南山区科技园科苑路18号未来矩阵大厦",
                        "未来矩阵科技有限公司专注于招聘 SaaS、组织协同与 AI 人才画像产品，为中大型企业提供招聘流程编排、人才库运营、校招数字化和用工流程管理解决方案。"
                ),
                new CompanySeed(
                        "recruiter@bluecloud.com",
                        "13800000003",
                        "蓝云数据招聘平台主管",
                        "蓝云数据智能有限公司",
                        "91310000MA1K654321",
                        "周雅宁",
                        "上海市浦东新区张江高科晨晖路88号",
                        "蓝云数据智能有限公司聚焦数据中台、商业智能和经营分析产品，为零售、制造和互联网客户输出数据治理、指标体系建设与可视化分析服务。"
                ),
                new CompanySeed(
                        "talent@smarthire.com",
                        "13800000004",
                        "智聘协同科技HR",
                        "智聘协同科技有限公司",
                        "91310115MA7A210043",
                        "林书瑶",
                        "杭州市余杭区梦想小镇互联网村7号楼",
                        "智聘协同科技有限公司面向企业招聘协同场景提供 ATS、面试排程、人才推荐和用工流程平台，强调流程自动化与多角色协同体验。"
                ),
                new CompanySeed(
                        "campus@huaxing.com",
                        "13800000008",
                        "华星互联招聘负责人",
                        "华星互联科技有限公司",
                        "91330100MA1H888001",
                        "赵文博",
                        "武汉市洪山区光谷软件园A2栋",
                        "华星互联科技有限公司深耕校园招聘、雇主品牌传播和高校合作服务，围绕校招宣讲、简历筛选和渠道运营建立数字化平台。"
                ),
                new CompanySeed(
                        "hire@yunsheng.com",
                        "13800000009",
                        "云盛数字HR",
                        "云盛数字科技有限公司",
                        "91510100MA6C999002",
                        "何雨辰",
                        "成都市高新区天府软件园E6栋",
                        "云盛数字科技有限公司为成长型企业提供云上运维、客户增长和内容运营工具，兼顾业务中台建设与技术服务交付。"
                ),
                new CompanySeed(
                        "jobs@xinghangai.com",
                        "13800000010",
                        "星航智联招聘经理",
                        "星航智联科技有限公司",
                        "91320115MA2P668810",
                        "蒋若晨",
                        "南京市建邺区江东中路339号",
                        "星航智联科技有限公司关注云原生平台、智能测试和企业研发效能提升，服务对象覆盖制造、汽车和互联网软件团队。"
                ),
                new CompanySeed(
                        "talent@qichensoft.com",
                        "13800000011",
                        "启辰工业软件HR",
                        "启辰工业软件有限公司",
                        "91320594MA27Q68012",
                        "孙志恒",
                        "苏州市工业园区金鸡湖大道89号",
                        "启辰工业软件有限公司聚焦工业软件、设备联网和实施交付，为制造企业提供生产管理系统、质量追溯和数据采集方案。"
                ),
                new CompanySeed(
                        "hr@meddomain.com",
                        "13800000012",
                        "知域医疗HR",
                        "知域医疗信息科技有限公司",
                        "91310110MA1G889120",
                        "郭静怡",
                        "上海市杨浦区国定东路275号",
                        "知域医疗信息科技有限公司专注医疗信息化、数据治理和质量体系建设，服务医院、互联网医疗和器械企业的数字化升级。"
                ),
                new CompanySeed(
                        "join@beichenretail.com",
                        "13800000013",
                        "北辰零售科技招聘经理",
                        "北辰零售科技有限公司",
                        "91330108MA2KB77139",
                        "罗景澄",
                        "杭州市滨江区江陵路560号",
                        "北辰零售科技有限公司为零售品牌提供会员增长、CRM、营销自动化和电商运营平台，强调数据驱动和精细化运营。"
                ),
                new CompanySeed(
                        "career@zhifengcloud.com",
                        "13800000014",
                        "智峰云服招聘经理",
                        "智峰云服科技有限公司",
                        "91110108MA01D77141",
                        "彭若凡",
                        "北京市海淀区上地十街10号",
                        "智峰云服科技有限公司提供企业人力资源数字化、招聘运营和共享服务平台，覆盖招聘顾问、组织协同与服务质量管理等场景。"
                ),
                new CompanySeed(
                        "jobs@heyuelife.com",
                        "13800000021",
                        "和悦餐饮招聘经理",
                        "和悦餐饮管理有限公司",
                        "91310112MA1R210021",
                        "韩知夏",
                        "上海市闵行区申长路869号",
                        "和悦餐饮管理有限公司深耕中式简餐与社区餐饮连锁运营，围绕门店营运、供应链管理、品牌活动和会员服务建立标准化经营体系。"
                ),
                new CompanySeed(
                        "hr@chenchuanlogistics.com",
                        "13800000022",
                        "晨川物流招聘经理",
                        "晨川物流供应链有限公司",
                        "91320113MA2N330022",
                        "方启明",
                        "南京市江宁区秣周东路12号",
                        "晨川物流供应链有限公司专注城配运输、仓配一体和冷链履约服务，为商超、餐饮和快消客户提供高时效物流解决方案。"
                ),
                new CompanySeed(
                        "talent@qinghefresh.com",
                        "13800000023",
                        "青禾生鲜招聘经理",
                        "青禾生鲜零售有限公司",
                        "91330110MA2M440023",
                        "苏明岚",
                        "杭州市拱墅区上塘路288号",
                        "青禾生鲜零售有限公司围绕社区生鲜门店、即时零售和会员服务开展经营，强调供应稳定、商品周转与门店标准化运营。"
                ),
                new CompanySeed(
                        "join@lanxitravel.com",
                        "13800000024",
                        "岚汐文旅招聘经理",
                        "岚汐文旅运营有限公司",
                        "91350203MA8P550024",
                        "许听澜",
                        "厦门市思明区环岛东路188号",
                        "岚汐文旅运营有限公司聚焦景区运营、节庆活动和文旅品牌合作，服务海滨景区、休闲街区与城市文旅消费场景。"
                ),
                new CompanySeed(
                        "career@anyihealth.com",
                        "13800000025",
                        "安颐养老招聘经理",
                        "安颐养老服务有限公司",
                        "91510108MA6D660025",
                        "陆清禾",
                        "成都市高新区益州大道399号",
                        "安颐养老服务有限公司为社区养老中心、照护站和康复服务站提供运营支持，围绕护理、康复和长者服务打造综合养老服务体系。"
                ),
                new CompanySeed(
                        "jobs@yaochenhome.com",
                        "13800000026",
                        "耀辰家居招聘经理",
                        "耀辰家居制造有限公司",
                        "91440605MA4L770026",
                        "顾承宇",
                        "佛山市南海区狮山镇博爱东路66号",
                        "耀辰家居制造有限公司专注成品家居和定制家居生产，覆盖生产计划、质量管理、渠道销售和交付服务等制造经营环节。"
                ),
                new CompanySeed(
                        "hr@jiasuifood.com",
                        "13800000027",
                        "嘉穗食品招聘经理",
                        "嘉穗食品供应有限公司",
                        "91420112MA4K880027",
                        "梁知远",
                        "武汉市东西湖区金银湖路128号",
                        "嘉穗食品供应有限公司服务商超、餐饮与便利渠道，主营预制食品与常温食品供应，注重品控、供应稳定和渠道覆盖效率。"
                ),
                new CompanySeed(
                        "talent@henglangsports.com",
                        "13800000028",
                        "恒朗体育招聘经理",
                        "恒朗体育发展有限公司",
                        "91320594MA27Q990028",
                        "程一凡",
                        "苏州市工业园区星港街198号",
                        "恒朗体育发展有限公司经营综合体育场馆、青少体培课程与会员活动，围绕场馆服务、课程咨询和赛事社群构建运营体系。"
                ),
                new CompanySeed(
                        "join@ruiyaedu.com",
                        "13800000029",
                        "睿芽教育招聘经理",
                        "睿芽教育服务有限公司",
                        "91510107MA6FA10029",
                        "温书宁",
                        "成都市武侯区人民南路四段27号",
                        "睿芽教育服务有限公司聚焦素养课程、校区运营和家校服务，围绕教务管理、招生咨询和学习服务打造稳定教学支持体系。"
                ),
                new CompanySeed(
                        "hr@lanshiproperty.com",
                        "13800000030",
                        "澜石物业招聘经理",
                        "澜石物业服务有限公司",
                        "91370203MA3C220030",
                        "宋景川",
                        "青岛市市南区香港中路32号",
                        "澜石物业服务有限公司服务商业综合体与写字楼项目，重点覆盖项目运营、客户服务、设备维保和现场品质管理。"
                )
        );
    }

    private List<JobseekerSeed> jobseekerSeeds() {
        return List.of(
                new JobseekerSeed(
                        "alice@example.com",
                        "13700000002",
                        "李雨桐",
                        "后端开发",
                        "深圳",
                        15000,
                        22000,
                        "本科",
                        3,
                        "女",
                        25,
                        "熟悉 Java、Spring Boot、MySQL 和 Redis，参与过招聘平台、流程协同和数据接口开发，能够独立完成接口设计、联调优化与问题排查。",
                        "华南理工大学",
                        "软件工程",
                        LocalDate.of(2018, 9, 1),
                        LocalDate.of(2022, 6, 30),
                        "主修软件工程、数据库系统、分布式系统与 Web 应用开发课程，参与过企业级项目实训与后端架构设计。",
                        List.of("Java", "Spring Boot", "MySQL", "Redis")
                ),
                new JobseekerSeed(
                        "bob@example.com",
                        "13700000005",
                        "王浩然",
                        "前端开发",
                        "杭州",
                        13000,
                        20000,
                        "本科",
                        3,
                        "男",
                        26,
                        "熟悉 Vue3、TypeScript、Element Plus 和前端工程化，做过招聘工作台、数据看板和移动端适配项目，重视交互体验与页面性能优化。",
                        "暨南大学",
                        "数字媒体技术",
                        LocalDate.of(2017, 9, 1),
                        LocalDate.of(2021, 6, 30),
                        "主修前端开发、交互设计、视觉表达与数据可视化课程，具备较强的页面实现与组件封装能力。",
                        List.of("Vue3", "TypeScript", "Element Plus", "Figma")
                ),
                new JobseekerSeed(
                        "charlie@example.com",
                        "13700000006",
                        "陈思远",
                        "产品经理",
                        "上海",
                        18000,
                        26000,
                        "硕士",
                        4,
                        "男",
                        27,
                        "具备 B 端产品规划、需求拆解、流程设计和跨团队推进经验，参与过招聘协同、数据分析和 CRM 产品迭代，能结合数据与访谈推动方案落地。",
                        "深圳大学",
                        "信息管理与信息系统",
                        LocalDate.of(2016, 9, 1),
                        LocalDate.of(2020, 6, 30),
                        "主修信息系统分析、产品设计方法、用户研究与商业分析课程，毕业后持续深耕 B 端产品设计。",
                        List.of("产品设计", "用户研究", "埋点分析", "CRM")
                ),
                new JobseekerSeed(
                        "diana@example.com",
                        "13700000007",
                        "赵嘉宁",
                        "数据分析",
                        "上海",
                        14000,
                        22000,
                        "硕士",
                        3,
                        "女",
                        26,
                        "熟悉 SQL、Python、统计分析和可视化报表建设，做过用户增长分析、经营看板和渠道复盘，能独立输出指标体系与分析结论。",
                        "华东师范大学",
                        "统计学",
                        LocalDate.of(2018, 9, 1),
                        LocalDate.of(2021, 6, 30),
                        "研究方向为商业分析、统计建模和数据可视化，具备较强的数据敏感度和结构化表达能力。",
                        List.of("SQL", "Python", "数据分析", "BI")
                ),
                new JobseekerSeed(
                        "sun@example.com",
                        "13700000008",
                        "孙若溪",
                        "运维开发",
                        "成都",
                        15000,
                        23000,
                        "本科",
                        4,
                        "男",
                        27,
                        "熟悉 Linux、Docker、CI/CD 和监控告警体系，参与过云上系统部署、稳定性治理和自动化运维建设，注重效率提升和故障闭环。",
                        "杭州电子科技大学",
                        "网络工程",
                        LocalDate.of(2015, 9, 1),
                        LocalDate.of(2019, 6, 30),
                        "主修操作系统、计算机网络、脚本开发与云平台基础课程，毕业后长期从事运维开发和平台稳定性工作。",
                        List.of("Linux", "Docker", "DevOps", "Kubernetes")
                ),
                new JobseekerSeed(
                        "lin@example.com",
                        "13700000009",
                        "林哲宇",
                        "UI设计",
                        "杭州",
                        12000,
                        18000,
                        "本科",
                        3,
                        "男",
                        25,
                        "熟悉 Figma、设计规范、界面视觉和交互表达，参与过招聘门户、企业后台和品牌传播项目，能够完成高保真设计与设计资产沉淀。",
                        "四川美术学院",
                        "视觉传达设计",
                        LocalDate.of(2017, 9, 1),
                        LocalDate.of(2021, 6, 30),
                        "主修视觉设计、交互设计、版式系统和品牌表达课程，具备完整的设计执行与协作经验。",
                        List.of("Figma", "用户研究", "产品设计", "TypeScript")
                ),
                new JobseekerSeed(
                        "gao@example.com",
                        "13700000010",
                        "高明轩",
                        "招聘运营",
                        "北京",
                        10000,
                        16000,
                        "本科",
                        3,
                        "男",
                        26,
                        "熟悉招聘流程、渠道管理、数据报表和协同推进，参与过雇主品牌活动、ATS 数据梳理和候选人转化优化，沟通执行能力较强。",
                        "浙江工商大学",
                        "电子商务",
                        LocalDate.of(2016, 9, 1),
                        LocalDate.of(2020, 6, 30),
                        "主修运营管理、用户增长、市场分析和商业数据处理课程，具备招聘运营与数据复盘思维。",
                        List.of("招聘运营", "渠道运营", "数据运营", "人力资源")
                )
        );
    }

    private List<JobSeed> jobSeeds() {
        return List.of(
                new JobSeed("hr@futuretech.com", "DEMO20260408001", "Java后端开发工程师", "后端开发", "深圳", 18000, 28000, "3年", "本科", 3,
                        List.of("五险一金", "补充医疗", "双休", "带薪年假", "技术培训", "年度体检"),
                        "招聘 SaaS 平台、企业工作台和人才画像服务",
                        "后端服务设计、接口开发、数据建模和性能优化",
                        "需求评审、联调测试、上线支持和问题复盘",
                        "Java、Spring Boot、MySQL、Redis",
                        "AI 辅助开发、消息队列或微服务治理"),
                new JobSeed("hr@futuretech.com", "DEMO20260408002", "测试开发工程师", "测试开发", "深圳", 15000, 22000, "2年", "本科", 2,
                        List.of("五险一金", "补充医疗", "双休", "带薪年假", "技术培训", "年度体检"),
                        "招聘系统核心链路、简历流程和消息通知模块",
                        "测试计划制定、自动化脚本开发、接口验证和质量指标建设",
                        "开发联调、缺陷定位、版本回归和发布验收",
                        "接口测试、自动化测试、JUnit、Postman",
                        "AI 测试、性能压测或测试平台建设"),
                new JobSeed("hr@futuretech.com", "DEMO20260408003", "AI测试工程师", "测试开发", "深圳", 18000, 26000, "3年", "本科", 2,
                        List.of("五险一金", "补充医疗", "双休", "带薪年假", "技术培训", "年度体检"),
                        "AI 问答评测、简历推荐模型和招聘智能助手功能",
                        "测试方案设计、Prompt 评测、结果分析和自动化校验机制建设",
                        "算法、产品、前后端联调以及线上效果复盘",
                        "测试用例设计、Prompt 评测、脚本开发、质量分析",
                        "大模型评测、RAG 或智能体测试"),

                new JobSeed("recruiter@bluecloud.com", "DEMO20260408004", "数据分析师", "数据分析", "上海", 14000, 22000, "3年", "本科", 2,
                        List.of("五险一金", "绩效奖金", "餐补", "弹性工作", "年度旅游", "数据学习基金"),
                        "经营分析平台、招聘漏斗看板和客户复购指标体系",
                        "SQL 分析、报表建设、指标拆解和专题分析输出",
                        "业务访谈、数据复盘、可视化展示和结论落地",
                        "SQL、Python、可视化报表与统计分析",
                        "A/B 测试、用户增长分析或机器学习"),
                new JobSeed("recruiter@bluecloud.com", "DEMO20260408005", "数据产品经理", "产品经理", "上海", 18000, 26000, "3年", "本科", 1,
                        List.of("五险一金", "绩效奖金", "餐补", "弹性工作", "年度旅游", "数据学习基金"),
                        "数据中台、业务指标体系和分析产品",
                        "需求梳理、指标模型设计、产品方案输出和版本推进",
                        "数据研发、分析师、业务团队协同和上线验收",
                        "指标体系、需求分析、数据建模和数据产品设计",
                        "埋点治理、实验平台或 B 端产品经验"),
                new JobSeed("recruiter@bluecloud.com", "DEMO20260408006", "BI开发工程师", "数据开发", "上海", 16000, 24000, "3年", "本科", 2,
                        List.of("五险一金", "绩效奖金", "餐补", "弹性工作", "年度旅游", "数据学习基金"),
                        "商业智能看板、数据集市和可视化分析门户",
                        "ETL 开发、主题模型搭建、BI 报表实现和口径治理",
                        "数据分析、产品、业务部门对齐和需求迭代",
                        "SQL、ETL、BI 工具与数据建模",
                        "DataV、数据资产管理或实时数仓"),

                new JobSeed("talent@smarthire.com", "DEMO20260408007", "Vue3前端开发工程师", "前端开发", "杭州", 14000, 22000, "3年", "本科", 2,
                        List.of("五险一金", "双休", "项目奖金", "设计评审成长机制", "下午茶", "年度体检"),
                        "招聘工作台、双栏简历编辑器和消息中心",
                        "页面实现、组件抽象、状态管理和性能优化",
                        "产品评审、设计走查、前后端联调和交互迭代",
                        "Vue3、TypeScript、组件化开发与工程化",
                        "性能优化、低代码或可视化编辑器"),
                new JobSeed("talent@smarthire.com", "DEMO20260408008", "UI设计师", "设计", "杭州", 12000, 18000, "2年", "本科", 1,
                        List.of("五险一金", "双休", "项目奖金", "设计评审成长机制", "下午茶", "年度体检"),
                        "招聘门户、企业后台和移动端投递流程",
                        "视觉方案设计、设计规范沉淀、交互细节优化和资产管理",
                        "产品讨论、研发走查、品牌传播和用户反馈优化",
                        "Figma、设计规范、界面视觉和交互表达",
                        "动效设计、设计系统或品牌延展"),
                new JobSeed("talent@smarthire.com", "DEMO20260408009", "产品经理", "产品经理", "杭州", 17000, 25000, "3年", "本科", 1,
                        List.of("五险一金", "双休", "项目奖金", "设计评审成长机制", "下午茶", "年度体检"),
                        "招聘协同平台、面试排程和状态流转能力",
                        "需求分析、原型设计、版本规划和效果复盘",
                        "研发、设计、测试、客户成功团队协同落地",
                        "需求分析、原型设计、项目推进和 B 端流程设计",
                        "AI 功能设计、数据驱动优化或 SaaS 经验"),

                new JobSeed("campus@huaxing.com", "DEMO20260408010", "校园渠道运营专员", "渠道运营", "武汉", 8000, 12000, "1年", "本科", 3,
                        List.of("五险一金", "寒暑假福利", "节日礼金", "导师带教", "活动补贴", "双休"),
                        "校园招聘渠道拓展、宣讲合作和高校资源维护",
                        "渠道洽谈、活动执行、数据复盘和资源沉淀",
                        "高校老师、学生社群、销售和雇主品牌团队协作",
                        "校园渠道拓展、活动执行、社群运营和数据复盘",
                        "雇主品牌、短视频运营或高校资源"),
                new JobSeed("campus@huaxing.com", "DEMO20260408011", "教育产品经理", "产品经理", "武汉", 16000, 22000, "3年", "本科", 1,
                        List.of("五险一金", "寒暑假福利", "节日礼金", "导师带教", "活动补贴", "双休"),
                        "校招平台、学生端投递流程和高校合作工具",
                        "需求洞察、功能规划、产品设计和项目推进",
                        "运营、教研、技术和校方合作方沟通协同",
                        "教育场景分析、产品规划、流程设计和用户研究",
                        "校招平台、内容产品或数据分析"),
                new JobSeed("campus@huaxing.com", "DEMO20260408012", "新媒体运营专员", "新媒体运营", "武汉", 7000, 11000, "1年", "大专", 2,
                        List.of("五险一金", "寒暑假福利", "节日礼金", "导师带教", "活动补贴", "双休"),
                        "校招传播内容、雇主品牌栏目和活动宣传矩阵",
                        "内容策划、选题排期、渠道投放和转化复盘",
                        "品牌、设计、校园渠道和商务团队协同",
                        "选题策划、内容分发、图文短视频运营和转化分析",
                        "AIGC 内容生产、直播运营或品牌传播"),

                new JobSeed("hire@yunsheng.com", "DEMO20260408013", "DevOps工程师", "运维开发", "成都", 18000, 28000, "3年", "本科", 2,
                        List.of("五险一金", "值班补贴", "餐补", "年度体检", "弹性调休", "云服务学习基金"),
                        "云上业务平台、发布流水线和自动化交付体系",
                        "CI/CD 建设、部署脚本优化、环境治理和效率提升",
                        "研发、测试、安全和运维团队协同",
                        "Linux、Docker、CI/CD 与自动化部署",
                        "Kubernetes、观测平台或混合云"),
                new JobSeed("hire@yunsheng.com", "DEMO20260408014", "Linux运维工程师", "运维", "成都", 13000, 20000, "2年", "本科", 2,
                        List.of("五险一金", "值班补贴", "餐补", "年度体检", "弹性调休", "云服务学习基金"),
                        "业务集群、网络环境和监控告警平台",
                        "服务器维护、故障排查、容量规划和监控优化",
                        "开发支持、客户问题协同和线上值守保障",
                        "Linux、网络排障、脚本开发和监控告警",
                        "数据库运维、容器平台或安全加固"),
                new JobSeed("hire@yunsheng.com", "DEMO20260408015", "技术支持工程师", "技术支持", "成都", 10000, 16000, "2年", "本科", 2,
                        List.of("五险一金", "值班补贴", "餐补", "年度体检", "弹性调休", "云服务学习基金"),
                        "SaaS 平台客户接入、日常使用和问题定位支持",
                        "问题受理、日志分析、配置排查和解决方案输出",
                        "客户成功、实施、研发和运维团队协同处理工单",
                        "客户问题分析、系统部署、日志排查和沟通协调",
                        "SQL 排障、实施经验或 SaaS 支持"),

                new JobSeed("jobs@xinghangai.com", "DEMO20260408016", "Golang后端开发工程师", "后端开发", "南京", 20000, 30000, "3年", "本科", 2,
                        List.of("五险一金", "年终奖金", "企业宿舍", "技术大会报销", "年度体检", "弹性办公"),
                        "云原生后台服务、研发效能平台和任务编排能力",
                        "Golang 服务开发、性能优化、缓存治理和接口设计",
                        "产品、平台、SRE 和测试团队联动协作",
                        "Golang、MySQL、Redis、接口设计",
                        "高并发服务、消息队列或云原生"),
                new JobSeed("jobs@xinghangai.com", "DEMO20260408017", "SRE工程师", "运维开发", "南京", 24000, 36000, "4年", "本科", 2,
                        List.of("五险一金", "年终奖金", "企业宿舍", "技术大会报销", "年度体检", "弹性办公"),
                        "关键业务稳定性、监控体系和故障应急响应机制",
                        "容量规划、监控建设、故障演练和稳定性治理",
                        "研发、运维、测试和管理团队协同改进",
                        "监控体系、容量评估、故障应急和稳定性治理",
                        "Chaos 工程、自动化运维或云成本优化"),
                new JobSeed("jobs@xinghangai.com", "DEMO20260408018", "云原生平台工程师", "平台开发", "南京", 22000, 34000, "4年", "本科", 1,
                        List.of("五险一金", "年终奖金", "企业宿舍", "技术大会报销", "年度体检", "弹性办公"),
                        "容器平台、研发交付平台和多环境资源管理体系",
                        "平台能力建设、工具链整合、权限治理和平台开发",
                        "研发、测试、安全和基础设施团队协同",
                        "Kubernetes、容器平台、DevOps 工具链和平台开发",
                        "Service Mesh、GitOps 或多云治理"),

                new JobSeed("talent@qichensoft.com", "DEMO20260408019", "测试工程师", "软件测试", "苏州", 10000, 15000, "2年", "本科", 2,
                        List.of("五险一金", "项目奖金", "餐补", "班车", "带薪年假", "年度体检"),
                        "工业软件模块、设备接口和业务流程质量验证",
                        "测试计划编写、功能验证、缺陷跟踪和回归测试",
                        "开发联调、实施验收和上线支持协作",
                        "测试计划、功能测试、缺陷跟踪与回归验证",
                        "自动化测试、医疗软件经验或质量体系"),
                new JobSeed("talent@qichensoft.com", "DEMO20260408020", "Java开发工程师", "后端开发", "苏州", 15000, 23000, "3年", "本科", 2,
                        List.of("五险一金", "项目奖金", "餐补", "班车", "带薪年假", "年度体检"),
                        "MES 周边系统、工艺流程平台和报表服务",
                        "后端接口开发、数据建模、业务流程实现和性能优化",
                        "实施顾问、测试、项目经理和客户需求方协同",
                        "Java、Spring Boot、数据库设计与接口开发",
                        "工业软件、MQ 或报表系统"),
                new JobSeed("talent@qichensoft.com", "DEMO20260408021", "实施顾问", "实施交付", "苏州", 12000, 18000, "2年", "本科", 3,
                        List.of("五险一金", "项目奖金", "餐补", "班车", "带薪年假", "年度体检"),
                        "制造企业项目实施、流程梳理和上线交付管理",
                        "需求调研、方案输出、培训支持和验收推进",
                        "客户现场、项目经理、开发和测试团队协同",
                        "需求调研、流程梳理、上线培训与客户沟通",
                        "ERP/MES 实施、SQL 或项目管理"),

                new JobSeed("hr@meddomain.com", "DEMO20260408022", "医疗信息产品经理", "产品经理", "上海", 20000, 30000, "4年", "本科", 1,
                        List.of("五险一金", "补充医疗", "节日福利", "年度体检", "双休", "专业培训"),
                        "医院信息平台、医生工作台和业务协同流程",
                        "需求研究、产品规划、原型设计和迭代推进",
                        "医院用户、实施顾问、研发和测试团队协同",
                        "医疗流程、需求管理、原型设计与项目推进",
                        "HIS、LIS、医保接口或数据治理"),
                new JobSeed("hr@meddomain.com", "DEMO20260408023", "数据治理工程师", "数据开发", "上海", 18000, 26000, "3年", "本科", 1,
                        List.of("五险一金", "补充医疗", "节日福利", "年度体检", "双休", "专业培训"),
                        "医疗数据标准、主数据体系和质量规则平台",
                        "标准制定、规则建设、ETL 梳理和质量闭环推进",
                        "数据、实施、产品和合规团队协同",
                        "数据标准、主数据、质量规则与 ETL",
                        "数据血缘、治理平台或隐私合规"),
                new JobSeed("hr@meddomain.com", "DEMO20260408024", "QA质量工程师", "质量管理", "上海", 13000, 19000, "2年", "本科", 1,
                        List.of("五险一金", "补充医疗", "节日福利", "年度体检", "双休", "专业培训"),
                        "医疗软件项目质量体系、过程审核和交付质量改进",
                        "质量计划执行、审核检查、风险识别和改进闭环",
                        "研发、测试、项目和合规团队协同推进",
                        "质量流程、审核检查、问题闭环与体系改进",
                        "ISO13485、GxP 或医疗器械经验"),

                new JobSeed("join@beichenretail.com", "DEMO20260408025", "电商运营专员", "电商运营", "杭州", 8000, 13000, "2年", "大专", 2,
                        List.of("五险一金", "绩效奖金", "商品内购", "下午茶", "团建基金", "弹性办公"),
                        "电商店铺、会员运营活动和商品增长策略",
                        "活动策划执行、商品管理、流量转化分析和复盘",
                        "内容、设计、供应链和客服团队协同",
                        "店铺运营、活动策划、商品管理与转化分析",
                        "直播电商、达人合作或 CRM"),
                new JobSeed("join@beichenretail.com", "DEMO20260408026", "CRM产品经理", "产品经理", "杭州", 18000, 28000, "3年", "本科", 1,
                        List.of("五险一金", "绩效奖金", "商品内购", "下午茶", "团建基金", "弹性办公"),
                        "会员体系、营销自动化和用户分层产品",
                        "产品方案设计、规则引擎梳理、版本规划和效果复盘",
                        "运营、数据、技术和品牌团队协同",
                        "会员体系、营销自动化、用户分层与旅程设计",
                        "CDP、私域运营或智能推荐"),
                new JobSeed("join@beichenretail.com", "DEMO20260408027", "数据运营专员", "数据运营", "杭州", 9000, 15000, "2年", "本科", 2,
                        List.of("五险一金", "绩效奖金", "商品内购", "下午茶", "团建基金", "弹性办公"),
                        "活动数据复盘、渠道评估和会员增长分析工作",
                        "看板维护、数据整理、经营分析和专项复盘输出",
                        "运营、市场、产品和管理团队协同",
                        "数据看板、活动复盘、用户增长与渠道分析",
                        "SQL、自动化报表或 A/B 测试"),

                new JobSeed("career@zhifengcloud.com", "DEMO20260408028", "招聘顾问", "人力资源", "北京", 12000, 20000, "2年", "本科", 3,
                        List.of("五险一金", "季度奖金", "培训补贴", "双休", "年度旅游", "带薪病假"),
                        "企业客户招聘服务、岗位画像梳理和候选人推荐流程",
                        "职位分析、人才寻访、候选人沟通和面试推进",
                        "客户经理、招聘运营和用人部门协同",
                        "岗位画像、候选人沟通、面试安排与客户维护",
                        "B 端销售、猎头经验或行业 mapping"),
                new JobSeed("career@zhifengcloud.com", "DEMO20260408029", "HRBP助理", "人力资源", "北京", 8000, 12000, "1年", "本科", 2,
                        List.of("五险一金", "季度奖金", "培训补贴", "双休", "年度旅游", "带薪病假"),
                        "组织沟通、培训支持和基础人事运营事务",
                        "数据整理、流程跟进、员工沟通和制度支持",
                        "业务负责人、行政、人事共享和招聘团队协同",
                        "组织沟通、入转调离、培训支持和数据整理",
                        "劳动关系、绩效支持或人事系统"),
                new JobSeed("career@zhifengcloud.com", "DEMO20260408030", "招聘运营专员", "招聘运营", "北京", 10000, 16000, "2年", "本科", 2,
                        List.of("五险一金", "季度奖金", "培训补贴", "双休", "年度旅游", "带薪病假"),
                        "招聘流程运营、渠道管理和数据报表体系建设",
                        "流程优化、渠道维护、数据复盘和协同推进",
                        "顾问团队、销售团队和用人经理之间的协作支持",
                        "招聘流程、数据报表、渠道管理和协同推进",
                        "ATS 系统、雇主品牌或自动化工具"),

                new JobSeed("jobs@heyuelife.com", "DEMO20260408031", "门店运营经理", "连锁运营", "上海", 15000, 22000, "3年", "大专", 2,
                        List.of("五险一金", "绩效奖金", "员工餐", "带薪年假", "节日福利", "晋升通道"),
                        "连锁餐饮门店经营目标、服务标准和营运流程优化",
                        "门店巡店、营业分析、人员排班和服务质量提升",
                        "区域督导、供应链、培训和门店店长协同",
                        "门店运营",
                        "餐饮连锁管理"),
                new JobSeed("jobs@heyuelife.com", "DEMO20260408032", "供应链采购专员", "采购管理", "上海", 9000, 14000, "2年", "大专", 2,
                        List.of("五险一金", "员工餐", "节日福利", "带薪年假", "绩效奖金", "专业培训"),
                        "餐饮原料采购、供应商管理和成本控制",
                        "采购询比价、订单跟进、库存协同和到货验收",
                        "门店、仓配、财务和供应商协同",
                        "采购谈判",
                        "供应商开发"),
                new JobSeed("jobs@heyuelife.com", "DEMO20260408033", "品牌活动策划", "市场策划", "上海", 10000, 16000, "2年", "本科", 1,
                        List.of("五险一金", "节日福利", "带薪年假", "下午茶", "绩效奖金", "团建基金"),
                        "餐饮品牌节庆活动、门店引流和会员拉新策划",
                        "活动创意、资源统筹、现场执行和效果复盘",
                        "品牌、门店、设计和渠道团队协同",
                        "活动策划",
                        "餐饮营销"),

                new JobSeed("hr@chenchuanlogistics.com", "DEMO20260408034", "仓储主管", "仓储管理", "南京", 12000, 18000, "3年", "大专", 2,
                        List.of("五险一金", "夜班补贴", "餐补", "带薪年假", "节日福利", "年度体检"),
                        "仓配中心库位管理、作业效率和库存准确率提升",
                        "收发存管理、班组安排、盘点优化和异常处理",
                        "运输、客服、采购和一线班组协同",
                        "仓储管理",
                        "WMS运营"),
                new JobSeed("hr@chenchuanlogistics.com", "DEMO20260408035", "运输调度专员", "物流调度", "南京", 9000, 13000, "2年", "大专", 3,
                        List.of("五险一金", "餐补", "高温补贴", "带薪年假", "节日福利", "班车"),
                        "城配线路调度、车辆效率和异常时效闭环",
                        "线路安排、司机沟通、异常协调和时效跟踪",
                        "仓库、司机、客服和客户现场协同",
                        "运输调度",
                        "线路优化"),
                new JobSeed("hr@chenchuanlogistics.com", "DEMO20260408036", "客户服务主管", "客户服务", "南京", 10000, 15000, "3年", "本科", 1,
                        List.of("五险一金", "餐补", "带薪年假", "节日福利", "年度体检", "晋升通道"),
                        "物流客户服务响应、投诉处理和履约体验提升",
                        "服务流程优化、客诉闭环、数据分析和团队带教",
                        "销售、调度、仓储和客户采购部门协同",
                        "客户服务管理",
                        "客诉处理"),

                new JobSeed("talent@qinghefresh.com", "DEMO20260408037", "生鲜品类采购专员", "采购管理", "杭州", 10000, 16000, "2年", "大专", 2,
                        List.of("五险一金", "商品内购", "节日福利", "带薪年假", "绩效奖金", "早班补贴"),
                        "蔬果肉禽品类采购、价格监控和供应稳定性管理",
                        "行情跟踪、供应商比价、采购下单和损耗控制",
                        "门店、仓储、供应商和品控团队协同",
                        "品类采购",
                        "生鲜供应链"),
                new JobSeed("talent@qinghefresh.com", "DEMO20260408038", "门店陈列督导", "门店运营", "杭州", 9000, 14000, "2年", "大专", 2,
                        List.of("五险一金", "商品内购", "带薪年假", "节日福利", "绩效奖金", "团建活动"),
                        "生鲜门店陈列标准、促销执行和损耗改善",
                        "巡店检查、陈列优化、活动落地和人员辅导",
                        "店长、采购、营运和市场团队协同",
                        "陈列管理",
                        "门店督导"),
                new JobSeed("talent@qinghefresh.com", "DEMO20260408039", "会员增长专员", "用户运营", "杭州", 11000, 17000, "2年", "本科", 1,
                        List.of("五险一金", "商品内购", "带薪年假", "节日福利", "绩效奖金", "下午茶"),
                        "会员拉新促活、社区团购转化和复购提升",
                        "会员活动执行、数据复盘、社群维护和权益运营",
                        "门店、市场、客服和商品团队协同",
                        "会员运营",
                        "社群增长"),

                new JobSeed("join@lanxitravel.com", "DEMO20260408040", "景区运营经理", "景区运营", "厦门", 14000, 21000, "3年", "本科", 1,
                        List.of("五险一金", "节日福利", "带薪年假", "交通补贴", "绩效奖金", "员工旅游"),
                        "景区接待、二次消费和现场运营体验提升",
                        "现场管理、活动统筹、服务标准建设和经营复盘",
                        "票务、商户、市场和游客服务团队协同",
                        "景区运营",
                        "文旅项目管理"),
                new JobSeed("join@lanxitravel.com", "DEMO20260408041", "活动策划执行", "活动策划", "厦门", 10000, 15000, "2年", "本科", 2,
                        List.of("五险一金", "节日福利", "带薪年假", "交通补贴", "绩效奖金", "员工旅游"),
                        "文旅节庆活动、主题市集和品牌传播执行",
                        "方案落地、供应商统筹、现场执行和传播复盘",
                        "设计、商户、市场和运营团队协同",
                        "活动执行",
                        "品牌传播"),
                new JobSeed("join@lanxitravel.com", "DEMO20260408042", "票务招商主管", "商务拓展", "厦门", 12000, 18000, "3年", "大专", 1,
                        List.of("五险一金", "节日福利", "带薪年假", "绩效奖金", "通讯补贴", "员工旅游"),
                        "票务渠道拓展、团购合作和旅行社资源维护",
                        "渠道洽谈、合作签约、政策执行和回款跟进",
                        "财务、运营、市场和渠道伙伴协同",
                        "渠道拓展",
                        "票务运营"),

                new JobSeed("career@anyihealth.com", "DEMO20260408043", "护理站主管", "护理管理", "成都", 13000, 19000, "3年", "大专", 1,
                        List.of("五险一金", "补充医疗", "带薪年假", "节日福利", "工作餐", "年度体检"),
                        "社区养老护理站服务质量、排班管理和家属沟通",
                        "护理流程监督、团队管理、服务评估和风险控制",
                        "医生、康复师、社工和家属协同",
                        "护理管理",
                        "养老服务"),
                new JobSeed("career@anyihealth.com", "DEMO20260408044", "康复治疗师", "康复治疗", "成都", 10000, 15000, "2年", "大专", 2,
                        List.of("五险一金", "补充医疗", "带薪年假", "节日福利", "工作餐", "专业培训"),
                        "长者康复训练、功能评估和个性化干预服务",
                        "康复评估、训练计划执行、记录反馈和家属指导",
                        "护理、医生、社工和家属协同",
                        "康复评估",
                        "老年康复"),
                new JobSeed("career@anyihealth.com", "DEMO20260408045", "社工运营专员", "社工服务", "成都", 8500, 12500, "1年", "本科", 2,
                        List.of("五险一金", "补充医疗", "带薪年假", "节日福利", "工作餐", "团建活动"),
                        "养老社区活动组织、长者陪伴和志愿者运营管理",
                        "活动策划、资源联动、档案整理和服务跟进",
                        "护理站、社区、志愿者和家属协同",
                        "社区活动组织",
                        "长者服务"),

                new JobSeed("jobs@yaochenhome.com", "DEMO20260408046", "生产计划专员", "生产管理", "佛山", 10000, 15000, "2年", "大专", 2,
                        List.of("五险一金", "包住", "餐补", "带薪年假", "节日福利", "年终奖金"),
                        "家居制造排产计划、产能平衡和交付节奏管理",
                        "排产编制、物料跟进、异常协调和交付复盘",
                        "采购、仓储、车间和销售团队协同",
                        "生产计划",
                        "PMC管理"),
                new JobSeed("jobs@yaochenhome.com", "DEMO20260408047", "质检主管", "质量管理", "佛山", 12000, 18000, "3年", "大专", 1,
                        List.of("五险一金", "包住", "餐补", "带薪年假", "节日福利", "年终奖金"),
                        "板式家居成品质检标准、过程稽核和质量改善",
                        "质量抽检、问题分析、标准执行和整改闭环",
                        "车间、工艺、采购和售后团队协同",
                        "质量管理",
                        "家具质检"),
                new JobSeed("jobs@yaochenhome.com", "DEMO20260408048", "渠道销售经理", "销售管理", "佛山", 14000, 22000, "3年", "本科", 2,
                        List.of("五险一金", "通讯补贴", "交通补贴", "带薪年假", "节日福利", "年终奖金"),
                        "经销商渠道拓展、年度销售目标和门店赋能管理",
                        "客户拜访、政策执行、回款跟进和渠道培训",
                        "工厂、设计、售后和经销商协同",
                        "渠道销售",
                        "家居建材销售"),

                new JobSeed("hr@jiasuifood.com", "DEMO20260408049", "区域销售代表", "区域销售", "武汉", 9000, 15000, "2年", "大专", 3,
                        List.of("五险一金", "交通补贴", "通讯补贴", "带薪年假", "节日福利", "绩效奖金"),
                        "商超和餐饮渠道食品铺货、陈列维护和销量提升",
                        "客户开发、订单跟进、陈列执行和回款维护",
                        "经销商、仓配、市场和终端门店协同",
                        "渠道销售",
                        "快消品销售"),
                new JobSeed("hr@jiasuifood.com", "DEMO20260408050", "品控专员", "品质管理", "武汉", 8000, 12000, "2年", "本科", 2,
                        List.of("五险一金", "工作餐", "带薪年假", "节日福利", "年度体检", "绩效奖金"),
                        "食品来料检验、生产质量监控和合规留样管理",
                        "抽检记录、异常处理、标准执行和报告整理",
                        "采购、生产、仓储和供应商协同",
                        "食品品控",
                        "质量体系"),
                new JobSeed("hr@jiasuifood.com", "DEMO20260408051", "供应链采购专员", "采购管理", "武汉", 9500, 14500, "2年", "本科", 2,
                        List.of("五险一金", "工作餐", "带薪年假", "节日福利", "绩效奖金", "专业培训"),
                        "食品原辅料采购、交期管理和供应稳定性建设",
                        "采购计划、供应商协同、订单跟进和成本分析",
                        "生产、仓储、品控和财务团队协同",
                        "采购执行",
                        "食品供应链"),

                new JobSeed("talent@henglangsports.com", "DEMO20260408052", "场馆运营主管", "场馆运营", "苏州", 11000, 17000, "3年", "大专", 1,
                        List.of("五险一金", "免费健身", "带薪年假", "节日福利", "绩效奖金", "员工活动"),
                        "体育场馆日常运营、课程排期和会员服务管理",
                        "场馆排班、现场管理、经营复盘和服务优化",
                        "教练、前台、市场和会员团队协同",
                        "场馆运营",
                        "体育服务管理"),
                new JobSeed("talent@henglangsports.com", "DEMO20260408053", "课程顾问", "课程顾问", "苏州", 9000, 14000, "1年", "大专", 3,
                        List.of("五险一金", "免费健身", "带薪年假", "节日福利", "绩效奖金", "专业培训"),
                        "青少体培课程咨询、试听转化和家长关系维护",
                        "咨询接待、试听安排、需求分析和签约跟进",
                        "教练、班主任、市场和家长协同",
                        "咨询转化",
                        "教培销售"),
                new JobSeed("talent@henglangsports.com", "DEMO20260408054", "社群活动执行", "社群运营", "苏州", 8500, 13000, "2年", "本科", 2,
                        List.of("五险一金", "免费健身", "带薪年假", "节日福利", "绩效奖金", "员工活动"),
                        "会员社群活跃、赛事活动组织和品牌互动传播",
                        "社群维护、活动执行、内容整理和数据复盘",
                        "教练、市场、运营和会员协同",
                        "社群运营",
                        "赛事执行"),

                new JobSeed("join@ruiyaedu.com", "DEMO20260408055", "教务主管", "教务管理", "成都", 12000, 18000, "3年", "本科", 1,
                        List.of("五险一金", "节日福利", "带薪年假", "绩效奖金", "午餐补贴", "专业培训"),
                        "校区排课、教学服务质量和家校沟通流程管理",
                        "排课统筹、教师协调、服务跟进和问题闭环",
                        "授课老师、班主任、咨询顾问和家长协同",
                        "教务管理",
                        "校区运营"),
                new JobSeed("join@ruiyaedu.com", "DEMO20260408056", "招生咨询师", "招生咨询", "成都", 9000, 15000, "1年", "大专", 3,
                        List.of("五险一金", "节日福利", "带薪年假", "绩效奖金", "午餐补贴", "专业培训"),
                        "课程咨询接待、试听转化和潜客跟进管理",
                        "咨询转化、试听安排、需求分析和续费跟进",
                        "教务、班主任、市场和家长协同",
                        "课程咨询",
                        "销售转化"),
                new JobSeed("join@ruiyaedu.com", "DEMO20260408057", "班主任", "学习管理", "成都", 8500, 13000, "2年", "本科", 2,
                        List.of("五险一金", "节日福利", "带薪年假", "绩效奖金", "午餐补贴", "专业培训"),
                        "学员学习跟进、出勤管理和家校服务体验提升",
                        "班级管理、学习反馈、续费沟通和服务跟踪",
                        "老师、教务、咨询和家长协同",
                        "班级管理",
                        "家校沟通"),

                new JobSeed("hr@lanshiproperty.com", "DEMO20260408058", "物业项目经理", "物业管理", "青岛", 14000, 22000, "4年", "大专", 1,
                        List.of("五险一金", "节日福利", "带薪年假", "年度体检", "绩效奖金", "工作餐"),
                        "商业物业项目服务品质、费用达成和现场安全管理",
                        "项目统筹、客户沟通、团队管理和品质提升",
                        "工程、客服、招商主管和商户协同",
                        "物业管理",
                        "商业项目运营"),
                new JobSeed("hr@lanshiproperty.com", "DEMO20260408059", "客服主管", "客户服务", "青岛", 9000, 14000, "2年", "大专", 2,
                        List.of("五险一金", "节日福利", "带薪年假", "年度体检", "绩效奖金", "工作餐"),
                        "商业楼宇客户服务响应、投诉处理和满意度提升",
                        "前台服务、客诉闭环、流程优化和班组带教",
                        "工程、保洁、安保和租户协同",
                        "客户服务管理",
                        "投诉处理"),
                new JobSeed("hr@lanshiproperty.com", "DEMO20260408060", "工程维修主管", "工程维修", "青岛", 11000, 17000, "3年", "大专", 2,
                        List.of("五险一金", "节日福利", "带薪年假", "年度体检", "绩效奖金", "工作餐"),
                        "商业项目设备巡检、维修保养和能耗管理",
                        "维保计划执行、故障处理、外包协调和台账管理",
                        "客服、安保、供应商和商户协同",
                        "设备维护",
                        "机电管理")
        );
    }

    private record CompanySeed(
            String email,
            String phone,
            String displayName,
            String companyName,
            String unifiedSocialCreditCode,
            String contactPerson,
            String address,
            String description
    ) {
    }

    private record JobseekerSeed(
            String email,
            String phone,
            String fullName,
            String desiredPositionCategory,
            String preferredCity,
            Integer expectedSalaryMin,
            Integer expectedSalaryMax,
            String highestEducation,
            Integer yearsOfExperience,
            String gender,
            Integer age,
            String summary,
            String schoolName,
            String major,
            LocalDate educationStartDate,
            LocalDate educationEndDate,
            String educationDescription,
            List<String> skills
    ) {
    }

    private record JobSeed(
            String companyEmail,
            String jobCode,
            String title,
            String category,
            String location,
            Integer salaryMin,
            Integer salaryMax,
            String experienceRequirement,
            String educationRequirement,
            Integer headcount,
            List<String> benefits,
            String businessContext,
            String coreDelivery,
            String collaborationScene,
            String requiredSkill,
            String bonusSkill
    ) {
    }
}
