package com.bishe.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.AdminDtos;
import com.bishe.recruitment.dto.AuthDtos;
import com.bishe.recruitment.dto.JobDtos;
import com.bishe.recruitment.entity.AdminActionLog;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Notification;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.enums.UserRole;
import com.bishe.recruitment.mapper.AdminActionLogMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.NotificationMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import com.bishe.recruitment.security.CustomUserDetailsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AdminServiceTests {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JobService jobService;

    @Autowired
    private UserSupportService userSupportService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CompanyProfileMapper companyProfileMapper;

    @Autowired
    private JobseekerProfileMapper jobseekerProfileMapper;

    @Autowired
    private JobPostMapper jobPostMapper;

    @Autowired
    private JobApplicationMapper jobApplicationMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private AdminActionLogMapper adminActionLogMapper;

    @Test
    void updateUserStatus_blocksDisabledAndDeletedAccountsFromAuthentication() {
        Long adminUserId = createAdminUser("admin-status-950001@example.com", "13900095001");
        Long jobseekerUserId = createJobseekerUser(
                "status-user-950001@example.com",
                "13700095001",
                "Status User 950001");

        AuthDtos.LoginRequest loginRequest = new AuthDtos.LoginRequest();
        loginRequest.setAccount("status-user-950001@example.com");
        loginRequest.setPassword("Password123");

        adminService.updateUserStatus(jobseekerUserId, "DISABLED", "Suspicious behavior", adminUserId);

        SysUser disabledUser = sysUserMapper.selectById(jobseekerUserId);
        assertThat(disabledUser.getStatus()).isEqualTo("DISABLED");
        assertThatThrownBy(() -> authService.login(loginRequest)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> customUserDetailsService.loadByUserId(jobseekerUserId))
                .isInstanceOf(BusinessException.class);

        adminService.updateUserStatus(jobseekerUserId, "ACTIVE", null, adminUserId);
        assertThat(authService.login(loginRequest).getUserId()).isEqualTo(jobseekerUserId);

        adminService.updateUserStatus(jobseekerUserId, "DELETED", "Invalid account cleanup", adminUserId);

        SysUser deletedUser = sysUserMapper.selectById(jobseekerUserId);
        assertThat(deletedUser.getStatus()).isEqualTo("DELETED");
        assertThatThrownBy(() -> authService.login(loginRequest)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> customUserDetailsService.loadByUserId(jobseekerUserId))
                .isInstanceOf(BusinessException.class);

        PageResponse<AdminDtos.JobseekerUserView> jobseekerPage = adminService.listJobseekers(null, null, 1, 50);
        assertThat(jobseekerPage.getRecords())
                .extracting(AdminDtos.JobseekerUserView::getUserId)
                .doesNotContain(jobseekerUserId);
    }

    @Test
    void auditCompany_requiresRejectReasonAndCreatesNotificationAndActionLog() {
        Long adminUserId = createAdminUser("admin-audit-950002@example.com", "13900095002");
        Long companyUserId = createCompanyUser(
                "company-audit-950002@example.com",
                "13800095002",
                "Audit Company 950002",
                CompanyAuditStatus.PENDING);

        assertThatThrownBy(() -> adminService.auditCompany(companyUserId, "REJECTED", null, adminUserId))
                .isInstanceOf(BusinessException.class);

        AdminDtos.CompanyUserView result = adminService.auditCompany(
                companyUserId,
                "REJECTED",
                "Missing supporting documents",
                adminUserId);

        CompanyProfile profile = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, companyUserId));
        Notification notification = latestNotification(companyUserId, "COMPANY_AUDIT");
        AdminActionLog actionLog = latestActionLog("COMPANY_AUDIT", companyUserId);

        assertThat(result.getAuditStatus()).isEqualTo("REJECTED");
        assertThat(profile.getAuditStatus()).isEqualTo("REJECTED");
        assertThat(notification).isNotNull();
        assertThat(notification.getContent()).contains("Missing supporting documents");
        assertThat(actionLog).isNotNull();
        assertThat(actionLog.getActionType()).isEqualTo("REJECTED");
        assertThat(actionLog.getReason()).isEqualTo("Missing supporting documents");
    }

    @Test
    void moderateJobByAdmin_requiresReasonAndLogicalDeleteRemovesJobFromVisibleLists() {
        Long adminUserId = createAdminUser("admin-job-950003@example.com", "13900095003");
        Long companyUserId = createCompanyUser(
                "company-job-950003@example.com",
                "13800095003",
                "Job Company 950003",
                CompanyAuditStatus.APPROVED);
        JobPost jobPost = createPublishedJob(companyUserId, "TEST-ADMIN-JOB-950003", "Admin Moderated Job");

        assertThatThrownBy(() -> jobService.moderateJobByAdmin(adminUserId, jobPost.getId(), "DELETE", " "))
                .isInstanceOf(BusinessException.class);

        jobService.moderateJobByAdmin(adminUserId, jobPost.getId(), "DELETE", "Policy violation");

        JobPost deletedJob = jobPostMapper.selectById(jobPost.getId());
        Notification notification = latestNotification(companyUserId, "JOB_STATUS");
        AdminActionLog actionLog = latestActionLog("JOB", jobPost.getId());

        assertThat(deletedJob.getDeletedFlag()).isEqualTo(1);
        assertThat(deletedJob.getDeletedBy()).isEqualTo(adminUserId);
        assertThat(deletedJob.getStatus()).isEqualTo(JobStatus.OFFLINE.name());
        assertThat(notification).isNotNull();
        assertThat(notification.getContent()).contains("Policy violation");
        assertThat(actionLog).isNotNull();
        assertThat(actionLog.getActionType()).isEqualTo("DELETE");
        assertThat(actionLog.getReason()).isEqualTo("Policy violation");

        JobDtos.JobSearchRequest searchRequest = new JobDtos.JobSearchRequest();
        searchRequest.setPageSize(50L);
        PageResponse<Map<String, Object>> searchResult = jobService.searchJobs(searchRequest, null);
        assertThat(searchResult.getRecords())
                .extracting(item -> String.valueOf(item.get("jobCode")))
                .doesNotContain("TEST-ADMIN-JOB-950003");

        PageResponse<AdminDtos.ManagedJobView> adminJobPage = jobService.listJobsForAdmin(null, null, null, null, 1, 50);
        assertThat(adminJobPage.getRecords())
                .extracting(AdminDtos.ManagedJobView::getId)
                .doesNotContain(jobPost.getId());
    }

    @Test
    void dashboard_returnsExpectedIncrementForUsersJobsAndApplications() {
        AdminDtos.DashboardView before = adminService.dashboard();

        Long companyUserId = createCompanyUser(
                "company-dashboard-950004@example.com",
                "13800095004",
                "Dashboard Company 950004",
                CompanyAuditStatus.APPROVED);
        Long jobseekerUserIdOne = createJobseekerUser(
                "jobseeker-dashboard-950041@example.com",
                "13700095041",
                "Dashboard Jobseeker 1");
        Long jobseekerUserIdTwo = createJobseekerUser(
                "jobseeker-dashboard-950042@example.com",
                "13700095042",
                "Dashboard Jobseeker 2");
        Long jobseekerUserIdThree = createJobseekerUser(
                "jobseeker-dashboard-950043@example.com",
                "13700095043",
                "Dashboard Jobseeker 3");
        JobPost jobPost = createPublishedJob(companyUserId, "TEST-ADMIN-JOB-950004", "Dashboard Job");

        createApplication(jobPost.getId(), companyUserId, jobseekerUserIdOne, "INTERVIEWING", 950041L);
        createApplication(jobPost.getId(), companyUserId, jobseekerUserIdTwo, "OFFERED", 950042L);
        createApplication(jobPost.getId(), companyUserId, jobseekerUserIdThree, "REJECTED", 950043L);

        AdminDtos.DashboardView after = adminService.dashboard();

        assertThat(after.getUserCount()).isEqualTo(before.getUserCount() + 4);
        assertThat(after.getCompanyUserCount()).isEqualTo(before.getCompanyUserCount() + 1);
        assertThat(after.getJobseekerUserCount()).isEqualTo(before.getJobseekerUserCount() + 3);
        assertThat(after.getPendingCompanyAuditCount()).isEqualTo(before.getPendingCompanyAuditCount());
        assertThat(after.getJobCount()).isEqualTo(before.getJobCount() + 1);
        assertThat(after.getApplicationCount()).isEqualTo(before.getApplicationCount() + 3);
        assertThat(after.getInterviewingCount()).isEqualTo(before.getInterviewingCount() + 1);
        assertThat(after.getOfferedCount()).isEqualTo(before.getOfferedCount() + 1);
        assertThat(after.getRejectedCount()).isEqualTo(before.getRejectedCount() + 1);
    }

    private Long createAdminUser(String email, String phone) {
        return userSupportService.createUser(email, phone, "Password123", "Admin User", UserRole.ADMIN).getId();
    }

    private Long createCompanyUser(String email, String phone, String companyName, CompanyAuditStatus auditStatus) {
        Long userId = userSupportService.createUser(email, phone, "Password123", companyName, UserRole.COMPANY).getId();

        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(userId);
        profile.setCompanyName(companyName);
        profile.setUnifiedSocialCreditCode("USCC" + phone);
        profile.setContactPerson("Contact " + phone.substring(phone.length() - 4));
        profile.setPhone(phone);
        profile.setEmail(email);
        profile.setAddress("Admin test address");
        profile.setDescription("Admin test company profile");
        profile.setAuditStatus(auditStatus.name());
        companyProfileMapper.insert(profile);
        return userId;
    }

    private Long createJobseekerUser(String email, String phone, String fullName) {
        Long userId = userSupportService.createUser(email, phone, "Password123", fullName, UserRole.JOBSEEKER).getId();

        JobseekerProfile profile = new JobseekerProfile();
        profile.setUserId(userId);
        profile.setFullName(fullName);
        profile.setPhone(phone);
        profile.setEmail(email);
        profile.setHighestEducation("Bachelor");
        profile.setDesiredPositionCategory("Backend");
        profile.setPreferredCity("Shanghai");
        profile.setYearsOfExperience(3);
        jobseekerProfileMapper.insert(profile);
        return userId;
    }

    private JobPost createPublishedJob(Long companyUserId, String jobCode, String title) {
        JobPost jobPost = new JobPost();
        jobPost.setCompanyUserId(companyUserId);
        jobPost.setJobCode(jobCode);
        jobPost.setTitle(title);
        jobPost.setCategory("Backend");
        jobPost.setLocation("Shanghai");
        jobPost.setSalaryMin(15000);
        jobPost.setSalaryMax(25000);
        jobPost.setExperienceRequirement("3 years");
        jobPost.setEducationRequirement("Bachelor");
        jobPost.setHeadcount(2);
        jobPost.setDescription("This is an admin moderation test job description that is long enough for publishing.");
        jobPost.setStatus(JobStatus.PUBLISHED.name());
        jobPost.setPublishedAt(LocalDateTime.now().minusHours(4));
        jobPost.setExpireAt(LocalDateTime.now().plusDays(7));
        jobPost.setDeletedFlag(0);
        jobPostMapper.insert(jobPost);
        return jobPost;
    }

    private void createApplication(Long jobId, Long companyUserId, Long jobseekerUserId, String status, Long resumeId) {
        JobApplication application = new JobApplication();
        application.setJobId(jobId);
        application.setCompanyUserId(companyUserId);
        application.setJobseekerUserId(jobseekerUserId);
        application.setResumeId(resumeId);
        application.setStatus(status);
        application.setAppliedAt(LocalDateTime.now().minusHours(2));
        jobApplicationMapper.insert(application);
    }

    private Notification latestNotification(Long userId, String type) {
        return notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getType, type)
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT 1"));
    }

    private AdminActionLog latestActionLog(String targetType, Long targetId) {
        return adminActionLogMapper.selectOne(new LambdaQueryWrapper<AdminActionLog>()
                .eq(AdminActionLog::getTargetType, targetType)
                .eq(AdminActionLog::getTargetId, targetId)
                .orderByDesc(AdminActionLog::getCreatedAt)
                .last("LIMIT 1"));
    }
}
