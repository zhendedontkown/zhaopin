package com.bishe.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.ApplicationDtos;
import com.bishe.recruitment.dto.ResumeDtos;
import com.bishe.recruitment.entity.ApplicationStatusLog;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.Notification;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.ResumeExperience;
import com.bishe.recruitment.entity.ResumeExtraSectionItem;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.mapper.ApplicationStatusLogMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.NotificationMapper;
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
    private static final Long APPLICATION_STATUS_USER_ID = 920005L;
    private static final Long APPLICATION_STATUS_COMPANY_ID = 930005L;
    private static final Long APPLICATION_FILTER_USER_ID = 920006L;
    private static final Long APPLICATION_FILTER_COMPANY_ID = 930006L;
    private static final Long APPLICATION_PRINT_USER_ID = 920007L;
    private static final Long APPLICATION_PRINT_COMPANY_ID = 930007L;
    private static final Long APPLICATION_PRINT_OTHER_COMPANY_ID = 930008L;
    private static final Long APPLICATION_VIEW_USER_ID = 920009L;
    private static final Long APPLICATION_VIEW_COMPANY_ID = 930009L;
    private static final Long APPLICATION_TRANSITION_USER_ID = 920010L;
    private static final Long APPLICATION_TRANSITION_COMPANY_ID = 930010L;
    private static final Long APPLICATION_IDEMPOTENT_USER_ID = 920011L;
    private static final Long APPLICATION_IDEMPOTENT_COMPANY_ID = 930011L;
    private static final Long APPLICATION_EXPORT_USER_ID = 920012L;
    private static final Long APPLICATION_EXPORT_COMPANY_ID = 930012L;
    private static final Long APPLICATION_REPLY_USER_ID = 920013L;
    private static final Long APPLICATION_REPLY_COMPANY_ID = 930013L;
    private static final Long SAVED_RESUME_UPDATE_USER_ID = 920015L;
    private static final Long SAVED_RESUME_RENAME_USER_ID = 920016L;
    private static final Long SAVED_RESUME_DELETE_USER_ID = 920017L;
    private static final Long SAVED_RESUME_DELETE_COMPANY_ID = 930017L;
    private static final Long SAVED_RESUME_DELETE_OTHER_USER_ID = 920018L;

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

    @Autowired
    private ApplicationStatusLogMapper applicationStatusLogMapper;

    @Autowired
    private NotificationMapper notificationMapper;

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

        ApplicationDtos.ApplicationRecordView firstApplication = applicationService.apply(APPLY_DUPLICATE_USER_ID, request);
        assertThat(firstApplication.getStatus()).isEqualTo("SUBMITTED");

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

    @Test
    void updateStatus_returnsUnifiedStatusFields() {
        resumeService.saveResume(APPLICATION_STATUS_USER_ID,
                createCompleteResumeRequest("状态联调同学", "status.flow@example.com", "13700001005"));
        JobPost jobPost = createPublishedJob(APPLICATION_STATUS_COMPANY_ID, "TEST-JOB-920005", "状态联调岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_STATUS_USER_ID, applyRequest);

        applicationService.getResumeForCompanyView(APPLICATION_STATUS_COMPANY_ID, application.getId(), true);

        ApplicationDtos.UpdateApplicationStatusRequest inviteRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        inviteRequest.setStatus("INTERVIEW_PENDING");
        inviteRequest.setRemark("请确认是否参加面试");
        applicationService.updateStatus(APPLICATION_STATUS_COMPANY_ID, application.getId(), inviteRequest);

        ApplicationDtos.InterviewResponseRequest interviewResponseRequest = new ApplicationDtos.InterviewResponseRequest();
        interviewResponseRequest.setDecision("ACCEPT");
        applicationService.respondToInterviewInvitation(
                APPLICATION_STATUS_USER_ID,
                application.getId(),
                interviewResponseRequest);

        ApplicationDtos.UpdateApplicationStatusRequest updateRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        updateRequest.setStatus("OFFERED");
        updateRequest.setRemark("已发出录用通知");

        ApplicationDtos.ApplicationRecordView updated = applicationService.updateStatus(
                APPLICATION_STATUS_COMPANY_ID,
                application.getId(),
                updateRequest);

        assertThat(updated.getStatus()).isEqualTo("OFFERED");
        assertThat(updated.getStatusText()).isEqualTo("已录用");
        assertThat(updated.getStatusDescription()).isEqualTo("候选人已被录用");
        assertThat(updated.getStatusUpdatedAt()).isNotNull();
        assertThat(updated.getStatusRemark()).isEqualTo("已发出录用通知");

        JobApplication persisted = jobApplicationMapper.selectById(application.getId());
        assertThat(persisted.getStatus()).isEqualTo("OFFERED");
        assertThat(persisted.getStatusRemark()).isEqualTo("已发出录用通知");
    }

    @Test
    void listByJobseeker_supportsStatusFilteringAndReturnsStatusCopy() {
        resumeService.saveResume(APPLICATION_FILTER_USER_ID,
                createCompleteResumeRequest("筛选状态同学", "status.filter@example.com", "13700001006"));
        JobPost viewedJob = createPublishedJob(APPLICATION_FILTER_COMPANY_ID, "TEST-JOB-920006-A", "已查看状态岗位");
        JobPost submittedJob = createPublishedJob(APPLICATION_FILTER_COMPANY_ID, "TEST-JOB-920006-B", "已投递状态岗位");

        ApplicationDtos.ApplyJobRequest viewedApplyRequest = new ApplicationDtos.ApplyJobRequest();
        viewedApplyRequest.setJobId(viewedJob.getId());
        ApplicationDtos.ApplicationRecordView viewedApplication = applicationService.apply(APPLICATION_FILTER_USER_ID, viewedApplyRequest);

        ApplicationDtos.ApplyJobRequest submittedApplyRequest = new ApplicationDtos.ApplyJobRequest();
        submittedApplyRequest.setJobId(submittedJob.getId());
        applicationService.apply(APPLICATION_FILTER_USER_ID, submittedApplyRequest);

        applicationService.getResumeForCompanyView(APPLICATION_FILTER_COMPANY_ID, viewedApplication.getId(), true);

        PageResponse<ApplicationDtos.ApplicationRecordView> viewedPage = applicationService.listByJobseeker(
                APPLICATION_FILTER_USER_ID,
                "VIEWED",
                1,
                10);
        PageResponse<ApplicationDtos.ApplicationRecordView> submittedPage = applicationService.listByJobseeker(
                APPLICATION_FILTER_USER_ID,
                "SUBMITTED",
                1,
                10);

        assertThat(viewedPage.getRecords()).hasSize(1);
        assertThat(viewedPage.getRecords().get(0).getStatus()).isEqualTo("VIEWED");
        assertThat(viewedPage.getRecords().get(0).getStatusText()).isEqualTo("企业已查看");
        assertThat(viewedPage.getRecords().get(0).getStatusDescription()).isEqualTo("企业已查看该简历");

        assertThat(submittedPage.getRecords()).hasSize(1);
        assertThat(submittedPage.getRecords().get(0).getStatus()).isEqualTo("SUBMITTED");
        assertThat(submittedPage.getRecords().get(0).getStatusText()).isEqualTo("已投递");
    }

    @Test
    void getResumeForCompanyView_prefersApplicationSnapshotOverCurrentResume() {
        ResumeDtos.ResumeSaveRequest firstResume = createCompleteResumeRequest("Snapshot V1", "print.snapshot@example.com", "13700001007");
        firstResume.setSummary("Snapshot summary v1");
        resumeService.saveResume(APPLICATION_PRINT_USER_ID, firstResume);
        JobPost jobPost = createPublishedJob(APPLICATION_PRINT_COMPANY_ID, "TEST-JOB-920007", "Resume Print Job");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_PRINT_USER_ID, applyRequest);

        ResumeDtos.ResumeSaveRequest updatedResume = createCompleteResumeRequest("Current Resume V2", "print.snapshot@example.com", "13700001007");
        updatedResume.setSummary("Current summary v2");
        resumeService.saveResume(APPLICATION_PRINT_USER_ID, updatedResume);

        JobApplication persisted = jobApplicationMapper.selectById(application.getId());
        assertThat(persisted.getResumeSnapshotJson()).isNotBlank();

        ApplicationDtos.ApplicationResumeView printView = applicationService.getResumeForCompanyView(
                APPLICATION_PRINT_COMPANY_ID,
                application.getId(),
                false);

        assertThat(printView.getSnapshotBased()).isTrue();
        assertThat(printView.getResumeSource()).isEqualTo("SNAPSHOT");

        @SuppressWarnings("unchecked")
        Map<String, Object> resume = (Map<String, Object>) printView.getResumeDetail().get("resume");
        assertThat(resume.get("fullName")).isEqualTo("Snapshot V1");
        assertThat(resume.get("summary")).isEqualTo("Snapshot summary v1");
    }

    @Test
    void getResumeForCompanyView_marksSubmittedApplicationAsViewedOnlyOnce() {
        resumeService.saveResume(APPLICATION_VIEW_USER_ID,
                createCompleteResumeRequest("首次查看同学", "first.view@example.com", "13700001009"));
        JobPost jobPost = createPublishedJob(APPLICATION_VIEW_COMPANY_ID, "TEST-JOB-920009", "首次查看岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_VIEW_USER_ID, applyRequest);

        assertThat(countStatusLogs(application.getId())).isEqualTo(1);
        assertThat(countApplicationStatusNotifications(APPLICATION_VIEW_USER_ID)).isZero();

        ApplicationDtos.ApplicationResumeView firstView = applicationService.getResumeForCompanyView(
                APPLICATION_VIEW_COMPANY_ID,
                application.getId(),
                true);
        assertThat(firstView.getResumeDetail()).isNotEmpty();

        JobApplication firstPersisted = jobApplicationMapper.selectById(application.getId());
        LocalDateTime firstViewedAt = firstPersisted.getViewedAt();
        assertThat(firstPersisted.getStatus()).isEqualTo("VIEWED");
        assertThat(firstViewedAt).isNotNull();
        assertThat(countStatusLogs(application.getId())).isEqualTo(2);
        assertThat(countApplicationStatusNotifications(APPLICATION_VIEW_USER_ID)).isEqualTo(1);

        ApplicationDtos.ApplicationResumeView secondView = applicationService.getResumeForCompanyView(
                APPLICATION_VIEW_COMPANY_ID,
                application.getId(),
                true);
        assertThat(secondView.getResumeDetail()).isNotEmpty();

        JobApplication secondPersisted = jobApplicationMapper.selectById(application.getId());
        assertThat(secondPersisted.getStatus()).isEqualTo("VIEWED");
        assertThat(secondPersisted.getViewedAt()).isEqualTo(firstViewedAt);
        assertThat(countStatusLogs(application.getId())).isEqualTo(2);
        assertThat(countApplicationStatusNotifications(APPLICATION_VIEW_USER_ID)).isEqualTo(1);
        assertThat(latestApplicationStatusNotification(APPLICATION_VIEW_USER_ID).getRelatedApplicationId()).isEqualTo(application.getId());
    }

    @Test
    void getResumeForCompanyView_keepsSubmittedStatusWhenResumeReadFails() {
        resumeService.saveResume(APPLICATION_PRINT_USER_ID,
                createCompleteResumeRequest("读取失败同学", "resume.fail@example.com", "13700001012"));
        JobPost jobPost = createPublishedJob(APPLICATION_PRINT_COMPANY_ID, "TEST-JOB-920012", "读取失败岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_PRINT_USER_ID, applyRequest);

        JobApplication brokenApplication = jobApplicationMapper.selectById(application.getId());
        brokenApplication.setResumeSnapshotJson("{");
        jobApplicationMapper.updateById(brokenApplication);

        assertThatThrownBy(() -> applicationService.getResumeForCompanyView(APPLICATION_PRINT_COMPANY_ID, application.getId(), true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("简历快照解析失败");

        JobApplication persisted = jobApplicationMapper.selectById(application.getId());
        assertThat(persisted.getStatus()).isEqualTo("SUBMITTED");
        assertThat(persisted.getViewedAt()).isNull();
        assertThat(countStatusLogs(application.getId())).isEqualTo(1);
        assertThat(countApplicationStatusNotifications(APPLICATION_PRINT_USER_ID)).isZero();
    }

    @Test
    void updateStatus_enforcesAllowedTransitionsAndIdempotency() {
        resumeService.saveResume(APPLICATION_TRANSITION_USER_ID,
                createCompleteResumeRequest("流转测试同学", "transition@example.com", "13700001010"));
        JobPost jobPost = createPublishedJob(APPLICATION_TRANSITION_COMPANY_ID, "TEST-JOB-920010", "流转测试岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_TRANSITION_USER_ID, applyRequest);

        applicationService.getResumeForCompanyView(APPLICATION_TRANSITION_COMPANY_ID, application.getId(), true);

        ApplicationDtos.UpdateApplicationStatusRequest inviteRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        inviteRequest.setStatus("INTERVIEW_PENDING");
        inviteRequest.setRemark("等待候选人确认");
        ApplicationDtos.ApplicationRecordView pending = applicationService.updateStatus(
                APPLICATION_TRANSITION_COMPANY_ID,
                application.getId(),
                inviteRequest);
        assertThat(pending.getStatus()).isEqualTo("INTERVIEW_PENDING");

        ApplicationDtos.InterviewResponseRequest acceptRequest = new ApplicationDtos.InterviewResponseRequest();
        acceptRequest.setDecision("ACCEPT");
        ApplicationDtos.ApplicationRecordView interviewing = applicationService.respondToInterviewInvitation(
                APPLICATION_TRANSITION_USER_ID,
                application.getId(),
                acceptRequest);
        assertThat(interviewing.getStatus()).isEqualTo("INTERVIEWING");

        ApplicationDtos.UpdateApplicationStatusRequest offeredRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        offeredRequest.setStatus("OFFERED");
        offeredRequest.setRemark("发出录用");
        ApplicationDtos.ApplicationRecordView offered = applicationService.updateStatus(
                APPLICATION_TRANSITION_COMPANY_ID,
                application.getId(),
                offeredRequest);
        assertThat(offered.getStatus()).isEqualTo("OFFERED");
        assertThat(offered.getStatusRemark()).isEqualTo("发出录用");

        ApplicationDtos.UpdateApplicationStatusRequest rejectedRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        rejectedRequest.setStatus("REJECTED");
        rejectedRequest.setRemark("回退驳回");
        assertThatThrownBy(() -> applicationService.updateStatus(
                APPLICATION_TRANSITION_COMPANY_ID,
                application.getId(),
                rejectedRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前状态不允许更新为目标状态");

        JobApplication persisted = jobApplicationMapper.selectById(application.getId());
        assertThat(persisted.getStatus()).isEqualTo("OFFERED");
        assertThat(persisted.getStatusRemark()).isEqualTo("发出录用");
        assertThat(countStatusLogs(application.getId())).isEqualTo(5);
    }

    @Test
    void updateStatus_sameStatusSubmissionIsIdempotent() {
        resumeService.saveResume(APPLICATION_IDEMPOTENT_USER_ID,
                createCompleteResumeRequest("幂等测试同学", "idempotent@example.com", "13700001011"));
        JobPost jobPost = createPublishedJob(APPLICATION_IDEMPOTENT_COMPANY_ID, "TEST-JOB-920011", "幂等测试岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_IDEMPOTENT_USER_ID, applyRequest);

        applicationService.getResumeForCompanyView(APPLICATION_IDEMPOTENT_COMPANY_ID, application.getId(), true);

        ApplicationDtos.UpdateApplicationStatusRequest firstRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        firstRequest.setStatus("REJECTED");
        firstRequest.setRemark("首轮筛选未通过");
        ApplicationDtos.ApplicationRecordView rejected = applicationService.updateStatus(
                APPLICATION_IDEMPOTENT_COMPANY_ID,
                application.getId(),
                firstRequest);
        assertThat(rejected.getStatus()).isEqualTo("REJECTED");

        JobApplication firstPersisted = jobApplicationMapper.selectById(application.getId());
        long statusLogCountAfterFirstUpdate = countStatusLogs(application.getId());
        long notificationCountAfterFirstUpdate = countApplicationStatusNotifications(APPLICATION_IDEMPOTENT_USER_ID);

        ApplicationDtos.UpdateApplicationStatusRequest secondRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        secondRequest.setStatus("REJECTED");
        secondRequest.setRemark("重复提交不应覆盖");
        ApplicationDtos.ApplicationRecordView secondResult = applicationService.updateStatus(
                APPLICATION_IDEMPOTENT_COMPANY_ID,
                application.getId(),
                secondRequest);
        assertThat(secondResult.getStatus()).isEqualTo("REJECTED");

        JobApplication secondPersisted = jobApplicationMapper.selectById(application.getId());
        assertThat(secondPersisted.getStatus()).isEqualTo("REJECTED");
        assertThat(secondPersisted.getStatusRemark()).isEqualTo("首轮筛选未通过");
        assertThat(secondPersisted.getViewedAt()).isEqualTo(firstPersisted.getViewedAt());
        assertThat(countStatusLogs(application.getId())).isEqualTo(statusLogCountAfterFirstUpdate);
        assertThat(countApplicationStatusNotifications(APPLICATION_IDEMPOTENT_USER_ID)).isEqualTo(notificationCountAfterFirstUpdate);
    }

    @Test
    void getResumeForCompanyView_rejectsCrossCompanyAccess() {
        resumeService.saveResume(APPLICATION_PRINT_USER_ID,
                createCompleteResumeRequest("Snapshot Guard", "print.guard@example.com", "13700001008"));
        JobPost jobPost = createPublishedJob(APPLICATION_PRINT_COMPANY_ID, "TEST-JOB-920008", "Resume Access Guard");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_PRINT_USER_ID, applyRequest);

        assertThatThrownBy(() -> applicationService.getResumeForCompanyView(APPLICATION_PRINT_OTHER_COMPANY_ID, application.getId(), true))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getResumeForCompanyView_doesNotMarkViewedWhenMarkViewedDisabled() {
        resumeService.saveResume(APPLICATION_EXPORT_USER_ID,
                createCompleteResumeRequest("导出同学", "resume.export@example.com", "13700001013"));
        JobPost jobPost = createPublishedJob(APPLICATION_EXPORT_COMPANY_ID, "TEST-JOB-920013", "导出只读岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_EXPORT_USER_ID, applyRequest);

        ApplicationDtos.ApplicationResumeView printView = applicationService.getResumeForCompanyView(
                APPLICATION_EXPORT_COMPANY_ID,
                application.getId(),
                false);
        assertThat(printView.getResumeDetail()).isNotEmpty();

        JobApplication persisted = jobApplicationMapper.selectById(application.getId());
        assertThat(persisted.getStatus()).isEqualTo("SUBMITTED");
        assertThat(persisted.getViewedAt()).isNull();
        assertThat(countStatusLogs(application.getId())).isEqualTo(1);
        assertThat(countApplicationStatusNotifications(APPLICATION_EXPORT_USER_ID)).isZero();
    }

    @Test
    void respondToInterviewInvitation_updatesStatusAndNotifiesCompany() {
        resumeService.saveResume(APPLICATION_REPLY_USER_ID,
                createCompleteResumeRequest("面试确认同学", "interview.reply@example.com", "13700001014"));
        JobPost jobPost = createPublishedJob(APPLICATION_REPLY_COMPANY_ID, "TEST-JOB-920014", "面试确认岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(APPLICATION_REPLY_USER_ID, applyRequest);

        applicationService.getResumeForCompanyView(APPLICATION_REPLY_COMPANY_ID, application.getId(), true);

        ApplicationDtos.UpdateApplicationStatusRequest inviteRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        inviteRequest.setStatus("INTERVIEW_PENDING");
        inviteRequest.setRemark("请确认是否参加面试");
        applicationService.updateStatus(APPLICATION_REPLY_COMPANY_ID, application.getId(), inviteRequest);

        ApplicationDtos.InterviewResponseRequest rejectRequest = new ApplicationDtos.InterviewResponseRequest();
        rejectRequest.setDecision("REJECT");
        ApplicationDtos.ApplicationRecordView updated = applicationService.respondToInterviewInvitation(
                APPLICATION_REPLY_USER_ID,
                application.getId(),
                rejectRequest);

        assertThat(updated.getStatus()).isEqualTo("VIEWED");
        assertThat(updated.getStatusRemark()).isEqualTo("候选人已拒绝面试邀请");
        assertThat(latestApplicationStatusNotification(APPLICATION_REPLY_COMPANY_ID).getRelatedApplicationId()).isEqualTo(application.getId());
    }

    @Test
    void updateSavedResume_overwritesExistingRecordAndKeepsNameAvailableForSelf() {
        ResumeDtos.CreateSavedResumeResponse created = resumeService.createSavedResume(
                SAVED_RESUME_UPDATE_USER_ID,
                createSavedResumeRequest(
                        "产品简历",
                        createCompleteResumeRequest("Saved Resume V1", "saved.update@example.com", "13700001016")));

        ResumeDtos.ResumeSaveRequest updatedDraft = createCompleteResumeRequest(
                "Saved Resume V2",
                "saved.update@example.com",
                "13700001016");
        updatedDraft.setSummary("updated summary");
        ResumeDtos.UpdateSavedResumeRequest updateRequest = createUpdateSavedResumeRequest("产品简历", updatedDraft);

        ResumeDtos.UpdateSavedResumeResponse updated = resumeService.updateSavedResume(
                SAVED_RESUME_UPDATE_USER_ID,
                created.getSavedResume().getId(),
                updateRequest);

        assertThat(updated.getSavedResume().getId()).isEqualTo(created.getSavedResume().getId());
        assertThat(updated.getSavedResume().getName()).isEqualTo("产品简历");

        ResumeDtos.SavedResumeDetailView detail = resumeService.getSavedResumeDetail(
                SAVED_RESUME_UPDATE_USER_ID,
                created.getSavedResume().getId());
        @SuppressWarnings("unchecked")
        Map<String, Object> resume = (Map<String, Object>) detail.getResumeDetail().get("resume");
        assertThat(resume.get("fullName")).isEqualTo("Saved Resume V2");
        assertThat(resume.get("summary")).isEqualTo("updated summary");

        PageResponse<ResumeDtos.SavedResumeSummaryView> page = resumeService.listSavedResumes(
                SAVED_RESUME_UPDATE_USER_ID,
                false,
                1,
                10);
        assertThat(page.getTotal()).isEqualTo(1);
    }

    @Test
    void updateSavedResume_rejectsDuplicateNameFromAnotherSavedResume() {
        ResumeDtos.CreateSavedResumeResponse first = resumeService.createSavedResume(
                SAVED_RESUME_RENAME_USER_ID,
                createSavedResumeRequest("简历A", createCompleteResumeRequest("Resume A", "saved.rename@example.com", "13700001017")));
        resumeService.createSavedResume(
                SAVED_RESUME_RENAME_USER_ID,
                createSavedResumeRequest("简历B", createCompleteResumeRequest("Resume B", "saved.rename@example.com", "13700001017")));

        ResumeDtos.UpdateSavedResumeRequest renameRequest = createUpdateSavedResumeRequest(
                "简历B",
                createCompleteResumeRequest("Resume A Updated", "saved.rename@example.com", "13700001017"));

        assertThatThrownBy(() -> resumeService.updateSavedResume(
                SAVED_RESUME_RENAME_USER_ID,
                first.getSavedResume().getId(),
                renameRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("简历名称已存在");
    }

    @Test
    void deleteSavedResume_removesSavedResumeAndKeepsApplicationSnapshotReadable() {
        ResumeDtos.CreateSavedResumeResponse created = resumeService.createSavedResume(
                SAVED_RESUME_DELETE_USER_ID,
                createSavedResumeRequest(
                        "投递简历",
                        createCompleteResumeRequest("Snapshot Delete", "saved.delete@example.com", "13700001018")));
        JobPost jobPost = createPublishedJob(SAVED_RESUME_DELETE_COMPANY_ID, "TEST-JOB-920017", "删除后快照岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        applyRequest.setSavedResumeId(created.getSavedResume().getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(SAVED_RESUME_DELETE_USER_ID, applyRequest);

        resumeService.deleteSavedResume(SAVED_RESUME_DELETE_USER_ID, created.getSavedResume().getId());

        assertThatThrownBy(() -> resumeService.getSavedResumeDetail(
                SAVED_RESUME_DELETE_USER_ID,
                created.getSavedResume().getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已保存简历不存在");

        ApplicationDtos.ApplicationResumeView printView = applicationService.getResumeForCompanyView(
                SAVED_RESUME_DELETE_COMPANY_ID,
                application.getId(),
                false);
        assertThat(printView.getSnapshotBased()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> resume = (Map<String, Object>) printView.getResumeDetail().get("resume");
        assertThat(resume.get("fullName")).isEqualTo("Snapshot Delete");
    }

    @Test
    void deleteSavedResume_rejectsCrossUserAccess() {
        ResumeDtos.CreateSavedResumeResponse created = resumeService.createSavedResume(
                SAVED_RESUME_DELETE_USER_ID,
                createSavedResumeRequest(
                        "仅本人可删",
                        createCompleteResumeRequest("Owned Resume", "saved.delete.owner@example.com", "13700001019")));

        assertThatThrownBy(() -> resumeService.deleteSavedResume(
                SAVED_RESUME_DELETE_OTHER_USER_ID,
                created.getSavedResume().getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已保存简历不存在");
    }

    @Test
    void updateStatus_withoutRemarkClearsPreviousRemark() {
        resumeService.saveResume(920014L,
                createCompleteResumeRequest("备注清理同学", "remark.clear@example.com", "13700001015"));
        JobPost jobPost = createPublishedJob(930014L, "TEST-JOB-920015", "备注清理岗位");

        ApplicationDtos.ApplyJobRequest applyRequest = new ApplicationDtos.ApplyJobRequest();
        applyRequest.setJobId(jobPost.getId());
        ApplicationDtos.ApplicationRecordView application = applicationService.apply(920014L, applyRequest);

        applicationService.getResumeForCompanyView(930014L, application.getId(), true);

        ApplicationDtos.UpdateApplicationStatusRequest inviteRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        inviteRequest.setStatus("INTERVIEW_PENDING");
        applicationService.updateStatus(930014L, application.getId(), inviteRequest);

        ApplicationDtos.InterviewResponseRequest acceptRequest = new ApplicationDtos.InterviewResponseRequest();
        acceptRequest.setDecision("ACCEPT");
        applicationService.respondToInterviewInvitation(920014L, application.getId(), acceptRequest);

        ApplicationDtos.UpdateApplicationStatusRequest rejectRequest = new ApplicationDtos.UpdateApplicationStatusRequest();
        rejectRequest.setStatus("REJECTED");
        ApplicationDtos.ApplicationRecordView rejected = applicationService.updateStatus(930014L, application.getId(), rejectRequest);

        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        assertThat(rejected.getStatusRemark()).isNull();

        JobApplication persisted = jobApplicationMapper.selectById(application.getId());
        assertThat(persisted.getStatus()).isEqualTo("REJECTED");
        assertThat(persisted.getStatusRemark()).isNull();
    }

    private long countStatusLogs(Long applicationId) {
        return applicationStatusLogMapper.selectCount(new LambdaQueryWrapper<ApplicationStatusLog>()
                .eq(ApplicationStatusLog::getApplicationId, applicationId));
    }

    private long countApplicationStatusNotifications(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getType, "APPLICATION_STATUS"));
    }

    private Notification latestApplicationStatusNotification(Long userId) {
        return notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getType, "APPLICATION_STATUS")
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT 1"));
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

    private ResumeDtos.CreateSavedResumeRequest createSavedResumeRequest(String name, ResumeDtos.ResumeSaveRequest draft) {
        ResumeDtos.CreateSavedResumeRequest request = new ResumeDtos.CreateSavedResumeRequest();
        request.setName(name);
        request.setDraft(draft);
        return request;
    }

    private ResumeDtos.UpdateSavedResumeRequest createUpdateSavedResumeRequest(String name, ResumeDtos.ResumeSaveRequest draft) {
        ResumeDtos.UpdateSavedResumeRequest request = new ResumeDtos.UpdateSavedResumeRequest();
        request.setName(name);
        request.setDraft(draft);
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
