package com.bishe.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.dto.ApplicationDtos;
import com.bishe.recruitment.dto.ResumeDtos;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.ResumeExperience;
import com.bishe.recruitment.entity.ResumeExtraSectionItem;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.ResumeExperienceMapper;
import com.bishe.recruitment.mapper.ResumeExtraSectionItemMapper;
import com.bishe.recruitment.mapper.ResumeMapper;
import com.bishe.recruitment.util.ResumeModuleConfigSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResumeAndApplicationServiceTests {

    private static final Long SAVE_CURRENT_USER_ID = 920001L;
    private static final Long SAVE_INVALID_USER_ID = 920002L;
    private static final Long APPLY_DUPLICATE_USER_ID = 920003L;
    private static final Long APPLY_DUPLICATE_COMPANY_ID = 930003L;
    private static final Long APPLY_INCOMPLETE_USER_ID = 920004L;
    private static final Long APPLY_INCOMPLETE_COMPANY_ID = 930004L;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private ResumeExperienceMapper resumeExperienceMapper;

    @Autowired
    private ResumeExtraSectionItemMapper resumeExtraSectionItemMapper;

    @Autowired
    private JobPostMapper jobPostMapper;

    @Autowired
    private JobApplicationMapper jobApplicationMapper;

    @Test
    void saveResume_persistsCurrentInternshipAndReturnsCurrentFlag() {
        ResumeDtos.ResumeSaveRequest request = createCompleteResumeRequest("实习同学", "shi.xi@example.com", "13700001001");
        request.setModuleConfig(moduleConfigWithVisible(ResumeModuleConfigSupport.INTERNSHIP));

        ResumeDtos.ExtraSectionItem internship = new ResumeDtos.ExtraSectionItem();
        internship.setTitle("后端开发实习生");
        internship.setSubtitle("未来科技");
        internship.setStartDate(LocalDate.of(2025, 7, 1));
        internship.setCurrent(true);
        internship.setDescription("参与招聘系统后端接口开发。");
        request.setInternships(List.of(internship));

        Map<String, Object> detail = resumeService.saveResume(SAVE_CURRENT_USER_ID, request);

        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, SAVE_CURRENT_USER_ID));
        assertThat(resume).isNotNull();

        ResumeExtraSectionItem persisted = resumeExtraSectionItemMapper.selectOne(new LambdaQueryWrapper<ResumeExtraSectionItem>()
                .eq(ResumeExtraSectionItem::getResumeId, resume.getId())
                .eq(ResumeExtraSectionItem::getSectionCode, "INTERNSHIP"));
        assertThat(persisted).isNotNull();
        assertThat(persisted.getCurrentFlag()).isTrue();
        assertThat(persisted.getEndDate()).isNull();

        @SuppressWarnings("unchecked")
        List<ResumeDtos.ExtraSectionItem> internships = (List<ResumeDtos.ExtraSectionItem>) detail.get("internships");
        assertThat(internships).hasSize(1);
        assertThat(internships.get(0).getCurrent()).isTrue();
        assertThat(internships.get(0).getEndDate()).isNull();
    }

    @Test
    void saveResume_rejectsIncompleteExperienceAndKeepsPreviousData() {
        ResumeDtos.ResumeSaveRequest validRequest = createCompleteResumeRequest("保留原数据", "resume.keep@example.com", "13700001002");
        validRequest.setExperiences(List.of(createExperience("未来科技", "Java开发", LocalDate.of(2023, 1, 1), LocalDate.of(2024, 12, 1))));
        resumeService.saveResume(SAVE_INVALID_USER_ID, validRequest);

        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, SAVE_INVALID_USER_ID));
        assertThat(resume).isNotNull();

        ResumeDtos.ResumeSaveRequest invalidRequest = createCompleteResumeRequest("保留原数据", "resume.keep@example.com", "13700001002");
        ResumeDtos.ExperienceItem invalidExperience = new ResumeDtos.ExperienceItem();
        invalidExperience.setCompanyName("未来科技");
        invalidRequest.setExperiences(List.of(invalidExperience));

        assertThatThrownBy(() -> resumeService.saveResume(SAVE_INVALID_USER_ID, invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工作经历第1条请填写职位");

        List<ResumeExperience> experiences = resumeExperienceMapper.selectList(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, resume.getId())
                .orderByAsc(ResumeExperience::getSortOrder));
        assertThat(experiences).hasSize(1);
        assertThat(experiences.get(0).getCompanyName()).isEqualTo("未来科技");
        assertThat(experiences.get(0).getJobTitle()).isEqualTo("Java开发");
    }

    @Test
    void apply_rejectsDuplicateApplicationWithinSevenDays() {
        resumeService.saveResume(APPLY_DUPLICATE_USER_ID,
                createCompleteResumeRequest("重复投递", "repeat.apply@example.com", "13700001003"));
        JobPost jobPost = createPublishedJob(APPLY_DUPLICATE_COMPANY_ID, "TEST-JOB-920003", "重复投递验证岗位");

        ApplicationDtos.ApplyJobRequest request = new ApplicationDtos.ApplyJobRequest();
        request.setJobId(jobPost.getId());

        Map<String, Object> firstApplication = applicationService.apply(APPLY_DUPLICATE_USER_ID, request);
        assertThat(firstApplication.get("status")).isEqualTo("SUBMITTED");

        assertThatThrownBy(() -> applicationService.apply(APPLY_DUPLICATE_USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同一岗位 7 天内仅允许投递一次");

        long applicationCount = jobApplicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getJobId, jobPost.getId())
                .eq(JobApplication::getJobseekerUserId, APPLY_DUPLICATE_USER_ID));
        assertThat(applicationCount).isEqualTo(1);
    }

    @Test
    void apply_rejectsIncompleteResumeBeforeInsert() {
        ResumeDtos.ResumeSaveRequest request = createCompleteResumeRequest("简历不完整", "resume.missing@example.com", "13700001004");
        request.setSkills(List.of());
        resumeService.saveResume(APPLY_INCOMPLETE_USER_ID, request);
        JobPost jobPost = createPublishedJob(APPLY_INCOMPLETE_COMPANY_ID, "TEST-JOB-920004", "完整度校验岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());

        assertThatThrownBy(() -> applicationService.apply(APPLY_INCOMPLETE_USER_ID, applyRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("技能标签");

        long applicationCount = jobApplicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getJobId, jobPost.getId())
                .eq(JobApplication::getJobseekerUserId, APPLY_INCOMPLETE_USER_ID));
        assertThat(applicationCount).isZero();
    }

    private ResumeDtos.ResumeSaveRequest createCompleteResumeRequest(String fullName, String email, String phone) {
        ResumeDtos.ResumeSaveRequest request = new ResumeDtos.ResumeSaveRequest();
        request.setTemplateCode("classic");
        request.setFullName(fullName);
        request.setGender("男");
        request.setBirthDate(LocalDate.of(2000, 1, 1));
        request.setDisplayAge(false);
        request.setPhone(phone);
        request.setEmail(email);
        request.setCity("上海");
        request.setExpectedCategory("后端开发");
        request.setExpectedSalaryMin(10000);
        request.setExpectedSalaryMax(18000);
        request.setYearsOfExperience(3);
        request.setSummary("熟悉 Java、Spring Boot 和 Vue3。");
        request.setEducations(List.of(createEducation("华东理工大学", "软件工程", "本科",
                LocalDate.of(2018, 9, 1), LocalDate.of(2022, 6, 1))));
        request.setSkills(List.of("Java", "Spring Boot"));
        return request;
    }

    private ResumeDtos.EducationItem createEducation(String schoolName, String major, String degree,
                                                     LocalDate startDate, LocalDate endDate) {
        ResumeDtos.EducationItem item = new ResumeDtos.EducationItem();
        item.setSchoolName(schoolName);
        item.setMajor(major);
        item.setDegree(degree);
        item.setStartDate(startDate);
        item.setEndDate(endDate);
        item.setDescription("主修后端开发与数据库课程。");
        item.setSortOrder(0);
        return item;
    }

    private ResumeDtos.ExperienceItem createExperience(String companyName, String jobTitle,
                                                       LocalDate startDate, LocalDate endDate) {
        ResumeDtos.ExperienceItem item = new ResumeDtos.ExperienceItem();
        item.setCompanyName(companyName);
        item.setJobTitle(jobTitle);
        item.setStartDate(startDate);
        item.setEndDate(endDate);
        item.setDescription("负责接口开发与联调。");
        item.setSortOrder(0);
        return item;
    }

    private List<ResumeDtos.ModuleConfigItem> moduleConfigWithVisible(String visibleCode) {
        List<ResumeDtos.ModuleConfigItem> items = new ArrayList<>();
        for (ResumeDtos.ModuleConfigItem defaultItem : ResumeModuleConfigSupport.defaultModuleConfig()) {
            ResumeDtos.ModuleConfigItem item = new ResumeDtos.ModuleConfigItem();
            item.setCode(defaultItem.getCode());
            item.setLabel(defaultItem.getLabel());
            item.setVisible(defaultItem.getVisible());
            item.setOrder(defaultItem.getOrder());
            if (defaultItem.getCode().equals(visibleCode)) {
                item.setVisible(true);
            }
            items.add(item);
        }
        return items;
    }

    private JobPost createPublishedJob(Long companyUserId, String jobCode, String title) {
        JobPost jobPost = new JobPost();
        jobPost.setCompanyUserId(companyUserId);
        jobPost.setJobCode(jobCode);
        jobPost.setTitle(title);
        jobPost.setCategory("后端开发");
        jobPost.setLocation("上海");
        jobPost.setSalaryMin(10000);
        jobPost.setSalaryMax(18000);
        jobPost.setExperienceRequirement("3年");
        jobPost.setEducationRequirement("本科");
        jobPost.setHeadcount(1);
        jobPost.setDescription("用于后端业务测试的岗位描述，长度足够覆盖保存和投递链路验证。");
        jobPost.setStatus(JobStatus.PUBLISHED.name());
        jobPost.setPublishedAt(LocalDateTime.now().minusDays(1));
        jobPost.setExpireAt(LocalDateTime.now().plusDays(7));
        jobPostMapper.insert(jobPost);
        return jobPost;
    }
}
