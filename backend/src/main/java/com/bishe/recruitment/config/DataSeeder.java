package com.bishe.recruitment.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.entity.ChatMessage;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.Conversation;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.ResumeEducation;
import com.bishe.recruitment.entity.ResumeSkill;
import com.bishe.recruitment.entity.SkillDict;
import com.bishe.recruitment.entity.SysRole;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.enums.ApplicationStatus;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.enums.UserRole;
import com.bishe.recruitment.mapper.ChatMessageMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.ConversationMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeEducationMapper resumeEducationMapper;
    private final ResumeSkillMapper resumeSkillMapper;
    private final SkillDictMapper skillDictMapper;
    private final JobPostMapper jobPostMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserSupportService userSupportService;

    public DataSeeder(SysRoleMapper sysRoleMapper, SysUserMapper sysUserMapper, CompanyProfileMapper companyProfileMapper,
                      JobseekerProfileMapper jobseekerProfileMapper, ResumeMapper resumeMapper,
                      ResumeEducationMapper resumeEducationMapper, ResumeSkillMapper resumeSkillMapper,
                      SkillDictMapper skillDictMapper, JobPostMapper jobPostMapper,
                      JobApplicationMapper jobApplicationMapper, ConversationMapper conversationMapper,
                      ChatMessageMapper chatMessageMapper, UserSupportService userSupportService) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserMapper = sysUserMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.resumeMapper = resumeMapper;
        this.resumeEducationMapper = resumeEducationMapper;
        this.resumeSkillMapper = resumeSkillMapper;
        this.skillDictMapper = skillDictMapper;
        this.jobPostMapper = jobPostMapper;
        this.jobApplicationMapper = jobApplicationMapper;
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
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
                "Java", "Spring Boot", "MySQL", "MyBatis Plus", "Vue3",
                "TypeScript", "Element Plus", "Redis", "Linux", "Docker",
                "HTML", "CSS", "JavaScript", "Git", "RESTful API");
        for (String skill : skills) {
            if (skillDictMapper.selectCount(new LambdaQueryWrapper<SkillDict>().eq(SkillDict::getSkillName, skill)) == 0) {
                SkillDict dict = new SkillDict();
                dict.setSkillName(skill);
                dict.setCategory("默认");
                skillDictMapper.insert(dict);
            }
        }
    }

    private void seedAdmin() {
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, "admin@recruitment.local")) == 0) {
            userSupportService.createUser("admin@recruitment.local", "13900000000", DEFAULT_PASSWORD, "系统管理员", UserRole.ADMIN);
        }
    }

    private void seedDemoData() {
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, "hr@futuretech.com")) == 0) {
            SysUser companyUser = userSupportService.createUser("hr@futuretech.com", "13800000001", DEFAULT_PASSWORD, "未来科技HR", UserRole.COMPANY);

            CompanyProfile companyProfile = new CompanyProfile();
            companyProfile.setUserId(companyUser.getId());
            companyProfile.setCompanyName("未来科技有限公司");
            companyProfile.setUnifiedSocialCreditCode("91310000MA1K123456");
            companyProfile.setContactPerson("张经理");
            companyProfile.setPhone("13800000001");
            companyProfile.setEmail("hr@futuretech.com");
            companyProfile.setAddress("深圳南山区科技园");
            companyProfile.setDescription("专注于企业招聘平台与数字化人力协同系统建设。");
            companyProfile.setAuditStatus(CompanyAuditStatus.APPROVED.name());
            companyProfileMapper.insert(companyProfile);

            JobPost jobPost = new JobPost();
            jobPost.setCompanyUserId(companyUser.getId());
            jobPost.setJobCode("JOB" + LocalDate.now().toString().replace("-", "") + "0001");
            jobPost.setTitle("Java后端开发工程师");
            jobPost.setCategory("后端开发");
            jobPost.setLocation("深圳");
            jobPost.setSalaryMin(12000);
            jobPost.setSalaryMax(20000);
            jobPost.setExperienceRequirement("3年");
            jobPost.setEducationRequirement("本科");
            jobPost.setHeadcount(3);
            jobPost.setDescription("负责招聘系统后台服务开发，参与 Spring Boot、MyBatis Plus、MySQL、Vue3 相关业务联调与优化，覆盖岗位管理、简历投递、推荐匹配、消息通知等核心模块建设，并持续参与接口设计、性能优化与上线支持工作。");
            jobPost.setStatus(JobStatus.PUBLISHED.name());
            jobPost.setPublishedAt(LocalDateTime.now().minusDays(2));
            jobPost.setExpireAt(LocalDateTime.now().plusDays(30));
            jobPostMapper.insert(jobPost);
        }

        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, "alice@example.com")) == 0) {
            SysUser jobseekerUser = userSupportService.createUser("alice@example.com", "13700000002", DEFAULT_PASSWORD, "李同学", UserRole.JOBSEEKER);

            JobseekerProfile profile = new JobseekerProfile();
            profile.setUserId(jobseekerUser.getId());
            profile.setFullName("李同学");
            profile.setPhone("13700000002");
            profile.setEmail("alice@example.com");
            profile.setDesiredPositionCategory("后端开发");
            profile.setPreferredCity("深圳");
            profile.setExpectedSalaryMin(10000);
            profile.setExpectedSalaryMax(18000);
            profile.setHighestEducation("本科");
            profile.setYearsOfExperience(3);
            jobseekerProfileMapper.insert(profile);

            Resume resume = new Resume();
            resume.setUserId(jobseekerUser.getId());
            resume.setTemplateCode("classic");
            resume.setFullName("李同学");
            resume.setPhone("13700000002");
            resume.setEmail("alice@example.com");
            resume.setCity("深圳");
            resume.setSummary("熟悉 Java、Spring Boot、Vue3 的全栈开发，具备招聘系统与管理后台项目经验。");
            resume.setExpectedCategory("后端开发");
            resume.setExpectedSalaryMin(10000);
            resume.setExpectedSalaryMax(18000);
            resume.setHighestEducation("本科");
            resume.setYearsOfExperience(3);
            resume.setCompletenessScore(85);
            resumeMapper.insert(resume);

            ResumeEducation education = new ResumeEducation();
            education.setResumeId(resume.getId());
            education.setSchoolName("华南理工大学");
            education.setMajor("软件工程");
            education.setDegree("本科");
            education.setStartDate(LocalDate.of(2018, 9, 1));
            education.setEndDate(LocalDate.of(2022, 6, 30));
            education.setDescription("主修 Java 开发、数据库原理、软件工程。");
            education.setSortOrder(0);
            resumeEducationMapper.insert(education);

            for (String skill : List.of("Java", "Spring Boot", "MySQL", "Vue3")) {
                SkillDict dict = skillDictMapper.selectOne(new LambdaQueryWrapper<SkillDict>().eq(SkillDict::getSkillName, skill));
                ResumeSkill resumeSkill = new ResumeSkill();
                resumeSkill.setResumeId(resume.getId());
                resumeSkill.setSkillId(dict == null ? null : dict.getId());
                resumeSkill.setSkillName(skill);
                resumeSkillMapper.insert(resumeSkill);
            }

            SysUser companyUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, "hr@futuretech.com"));
            JobPost jobPost = jobPostMapper.selectOne(new LambdaQueryWrapper<JobPost>()
                    .eq(JobPost::getCompanyUserId, companyUser.getId())
                    .last("limit 1"));

            if (jobPost != null) {
                JobApplication application = new JobApplication();
                application.setJobId(jobPost.getId());
                application.setCompanyUserId(companyUser.getId());
                application.setJobseekerUserId(jobseekerUser.getId());
                application.setResumeId(resume.getId());
                application.setStatus(ApplicationStatus.VIEWED.name());
                application.setAppliedAt(LocalDateTime.now().minusDays(1));
                application.setViewedAt(LocalDateTime.now().minusHours(12));
                jobApplicationMapper.insert(application);

                Conversation conversation = new Conversation();
                conversation.setCompanyUserId(companyUser.getId());
                conversation.setJobseekerUserId(jobseekerUser.getId());
                conversation.setCreatedAt(LocalDateTime.now().minusHours(10));
                conversation.setLastMessageAt(LocalDateTime.now().minusHours(2));
                conversationMapper.insert(conversation);

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setConversationId(conversation.getId());
                chatMessage.setSenderUserId(companyUser.getId());
                chatMessage.setReceiverUserId(jobseekerUser.getId());
                chatMessage.setContent("您好，我们已经查看了您的简历，方便进一步沟通项目经验吗？");
                chatMessage.setReadFlag(0);
                chatMessage.setCreatedAt(LocalDateTime.now().minusHours(2));
                chatMessageMapper.insert(chatMessage);
            }
        }
    }
}
