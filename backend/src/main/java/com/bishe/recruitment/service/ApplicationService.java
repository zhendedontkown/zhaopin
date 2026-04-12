package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.AdminDtos;
import com.bishe.recruitment.dto.ApplicationDtos;
import com.bishe.recruitment.entity.ApplicationStatusLog;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.SysUser;
import com.bishe.recruitment.enums.ApplicationStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.mapper.ApplicationStatusLogMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.SysUserMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApplicationService {

    private static final String LEGACY_ACCEPTED_STATUS = "ACCEPTED";
    private static final String COMPANY_OPERATOR_ROLE = "COMPANY";
    private static final String JOBSEEKER_OPERATOR_ROLE = "JOBSEEKER";

    private final JobApplicationMapper jobApplicationMapper;
    private final ApplicationStatusLogMapper applicationStatusLogMapper;
    private final JobPostMapper jobPostMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final ResumeService resumeService;
    private final NotificationService notificationService;

    public ApplicationService(JobApplicationMapper jobApplicationMapper, ApplicationStatusLogMapper applicationStatusLogMapper,
                              JobPostMapper jobPostMapper, CompanyProfileMapper companyProfileMapper,
                              JobseekerProfileMapper jobseekerProfileMapper, SysUserMapper sysUserMapper,
                              ResumeService resumeService, NotificationService notificationService) {
        this.jobApplicationMapper = jobApplicationMapper;
        this.applicationStatusLogMapper = applicationStatusLogMapper;
        this.jobPostMapper = jobPostMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.resumeService = resumeService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ApplicationDtos.ApplicationRecordView apply(Long jobseekerUserId, ApplicationDtos.ApplyJobRequest request) {
        Resume currentDraftResume = resumeService.getOrCreateByUserId(jobseekerUserId);
        Long appliedResumeId = currentDraftResume.getId(); /*
            throw new BusinessException("当前版本仅支持投递主简历");
        */
        Long appliedSavedResumeId = null;
        String appliedSavedResumeName = null;
        String resumeSnapshotJson;

        if (request.getSavedResumeId() != null) {
            var savedResume = resumeService.getCompleteSavedResumeOrThrow(jobseekerUserId, request.getSavedResumeId());
            appliedSavedResumeId = savedResume.getId();
            appliedSavedResumeName = savedResume.getName();
            resumeSnapshotJson = savedResume.getSnapshotJson();
        } else {
            Resume resume = resumeService.validateCompleteOrThrow(jobseekerUserId);
            if (request.getResumeId() != null && !request.getResumeId().equals(resume.getId())) {
                throw new BusinessException("当前版本仅支持投递当前草稿简历");
            }
            appliedResumeId = resume.getId();
            resumeSnapshotJson = resumeService.createResumeSnapshotJson(resume.getId());
        }
        JobPost jobPost = jobPostMapper.selectOne(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getId, request.getJobId())
                .and(wrapper -> wrapper.isNull(JobPost::getDeletedFlag).or().eq(JobPost::getDeletedFlag, 0))
                .last("FOR UPDATE"));
        if (jobPost == null || !JobStatus.PUBLISHED.name().equals(jobPost.getStatus())) {
            throw new BusinessException("岗位不存在或未发布");
        }
        if (jobPost.getExpireAt() != null && jobPost.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("岗位已过期，无法投递");
        }
        LocalDateTime duplicateDeadline = LocalDateTime.now().minusDays(7);
        long duplicateCount = jobApplicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getJobId, jobPost.getId())
                .eq(JobApplication::getJobseekerUserId, jobseekerUserId)
                .ge(JobApplication::getAppliedAt, duplicateDeadline));
        if (duplicateCount > 0) {
            throw new BusinessException("同一岗位 7 天内仅允许投递一次");
        }

        JobApplication application = new JobApplication();
        application.setJobId(jobPost.getId());
        application.setCompanyUserId(jobPost.getCompanyUserId());
        application.setJobseekerUserId(jobseekerUserId);
        application.setResumeId(appliedResumeId);
        application.setSavedResumeId(appliedSavedResumeId);
        application.setSavedResumeName(appliedSavedResumeName);
        application.setResumeSnapshotJson(resumeSnapshotJson);
        application.setStatus(ApplicationStatus.SUBMITTED.name());
        application.setAppliedAt(LocalDateTime.now());
        jobApplicationMapper.insert(application);

        createStatusLog(application.getId(), null, ApplicationStatus.SUBMITTED.name(), jobseekerUserId, JOBSEEKER_OPERATOR_ROLE, null);
        notificationService.createAndPush(
                jobPost.getCompanyUserId(),
                NotificationType.NEW_APPLICATION,
                "收到新的简历投递",
                "岗位 " + jobPost.getTitle() + " 收到了一份新的简历投递。");
        return buildApplicationView(application);
    }

    public PageResponse<ApplicationDtos.ApplicationRecordView> listByJobseeker(Long jobseekerUserId, String status,
                                                                               long pageNum, long pageSize) {
        Page<JobApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getJobseekerUserId, jobseekerUserId)
                .orderByDesc(JobApplication::getAppliedAt);
        applyStatusFilter(wrapper, status);

        Page<JobApplication> result = jobApplicationMapper.selectPage(page, wrapper);
        List<ApplicationDtos.ApplicationRecordView> records = result.getRecords().stream()
                .map(this::buildApplicationView)
                .toList();
        return PageResponse.<ApplicationDtos.ApplicationRecordView>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    public PageResponse<AdminDtos.AdminApplicationView> listForAdmin(String keyword,
                                                                     String status,
                                                                     LocalDateTime startAt,
                                                                     LocalDateTime endAt,
                                                                     long pageNum,
                                                                     long pageSize) {
        List<JobApplication> applications = jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .orderByDesc(JobApplication::getAppliedAt));
        Set<Long> jobIds = applications.stream()
                .map(JobApplication::getJobId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> companyUserIds = applications.stream()
                .map(JobApplication::getCompanyUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> jobseekerUserIds = applications.stream()
                .map(JobApplication::getJobseekerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, JobPost> jobMap = jobIds.isEmpty()
                ? Map.of()
                : jobPostMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(JobPost::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> companyNameMap = companyUserIds.isEmpty()
                ? Map.of()
                : companyProfileMapper.selectList(new LambdaQueryWrapper<CompanyProfile>()
                .in(CompanyProfile::getUserId, companyUserIds))
                .stream()
                .collect(Collectors.toMap(CompanyProfile::getUserId, CompanyProfile::getCompanyName, (left, right) -> left));
        Map<Long, JobseekerProfile> jobseekerProfileMap = jobseekerUserIds.isEmpty()
                ? Map.of()
                : jobseekerProfileMapper.selectList(new LambdaQueryWrapper<JobseekerProfile>()
                .in(JobseekerProfile::getUserId, jobseekerUserIds))
                .stream()
                .collect(Collectors.toMap(JobseekerProfile::getUserId, item -> item, (left, right) -> left));
        Map<Long, SysUser> userMap = jobseekerUserIds.isEmpty()
                ? Map.of()
                : sysUserMapper.selectBatchIds(jobseekerUserIds).stream()
                .collect(Collectors.toMap(SysUser::getId, item -> item, (left, right) -> left));

        List<AdminDtos.AdminApplicationView> records = applications.stream()
                .map(application -> buildAdminApplicationView(
                        application,
                        jobMap.get(application.getJobId()),
                        companyNameMap.get(application.getCompanyUserId()),
                        resolveJobseekerName(application.getJobseekerUserId(), jobseekerProfileMap, userMap)))
                .filter(view -> matchesAdminKeyword(keyword, view))
                .filter(view -> matchesAdminStatus(status, view.getStatus()))
                .filter(view -> matchesAppliedAtRange(startAt, endAt, view.getAppliedAt()))
                .toList();
        return paginate(records, pageNum, pageSize);
    }

    public PageResponse<ApplicationDtos.ApplicationRecordView> listByCompany(Long companyUserId, String status,
                                                                             long pageNum, long pageSize) {
        Page<JobApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getCompanyUserId, companyUserId)
                .orderByDesc(JobApplication::getAppliedAt);
        applyStatusFilter(wrapper, status);

        Page<JobApplication> result = jobApplicationMapper.selectPage(page, wrapper);
        List<ApplicationDtos.ApplicationRecordView> records = result.getRecords().stream()
                .map(this::buildApplicationView)
                .toList();
        return PageResponse.<ApplicationDtos.ApplicationRecordView>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    @Transactional
    public ApplicationDtos.ApplicationRecordView updateStatus(Long companyUserId, Long applicationId,
                                                              ApplicationDtos.UpdateApplicationStatusRequest request) {
        ApplicationStatus newStatus = normalizeStatus(request.getStatus());
        validateCompanyRequestedStatus(newStatus);
        JobApplication application = getCompanyApplicationOrThrow(companyUserId, applicationId, true);

        boolean changed = applyStatusChangeIfNeeded(
                application,
                newStatus,
                companyUserId,
                COMPANY_OPERATOR_ROLE,
                request.getRemark(),
                StatusChangeOptions.defaultOptions());

        if (changed) {
            notifyJobseekerOfCompanyStatusChange(application, newStatus);
        }

        return buildApplicationView(application);
    }

    @Transactional
    public ApplicationDtos.ApplicationRecordView respondToInterviewInvitation(Long jobseekerUserId, Long applicationId,
                                                                              ApplicationDtos.InterviewResponseRequest request) {
        InterviewResponse decision = normalizeInterviewResponse(request.getDecision());
        JobApplication application = getJobseekerApplicationOrThrow(jobseekerUserId, applicationId, true);
        ApplicationStatus currentStatus = resolveStoredStatus(application.getStatus());
        ApplicationStatus targetStatus = decision == InterviewResponse.ACCEPT
                ? ApplicationStatus.INTERVIEWING
                : ApplicationStatus.VIEWED;
        String remark = decision == InterviewResponse.ACCEPT
                ? "候选人已接受面试邀请"
                : "候选人已拒绝面试邀请";

        if (currentStatus == targetStatus && remark.equals(application.getStatusRemark())) {
            return buildApplicationView(application);
        }
        if (currentStatus != ApplicationStatus.INTERVIEW_PENDING) {
            throw new BusinessException("当前没有待确认的面试邀请");
        }

        boolean changed = applyStatusChangeIfNeeded(
                application,
                targetStatus,
                jobseekerUserId,
                JOBSEEKER_OPERATOR_ROLE,
                remark,
                StatusChangeOptions.defaultOptions());

        if (changed) {
            notifyCompanyOfInterviewResponse(application, decision);
        }

        return buildApplicationView(application);
    }

    public boolean existsApplicationBetween(Long companyUserId, Long jobseekerUserId) {
        return jobApplicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getCompanyUserId, companyUserId)
                .eq(JobApplication::getJobseekerUserId, jobseekerUserId)) > 0;
    }

    @Transactional
    public ApplicationDtos.ApplicationResumeView getResumeForCompanyView(Long companyUserId, Long applicationId, boolean markViewed) {
        JobApplication application = getCompanyApplicationOrThrow(companyUserId, applicationId, markViewed);
        JobPost jobPost = jobPostMapper.selectById(application.getJobId());
        CompanyProfile companyProfile = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, application.getCompanyUserId()));

        boolean snapshotBased = StringUtils.hasText(application.getResumeSnapshotJson());
        Map<String, Object> resumeDetail = snapshotBased
                ? resumeService.parseResumeSnapshotJson(application.getResumeSnapshotJson())
                : resumeService.getResumeDetailByResumeId(application.getResumeId());

        ApplicationDtos.ApplicationResumeView view = new ApplicationDtos.ApplicationResumeView();
        view.setApplicationId(application.getId());
        view.setResumeId(application.getResumeId());
        view.setSavedResumeId(application.getSavedResumeId());
        view.setSavedResumeName(application.getSavedResumeName());
        view.setJobTitle(jobPost == null ? "" : jobPost.getTitle());
        view.setCompanyName(companyProfile == null ? "" : companyProfile.getCompanyName());
        view.setSnapshotBased(snapshotBased);
        view.setResumeSource(snapshotBased ? "SNAPSHOT" : "CURRENT");
        view.setResumeDetail(resumeDetail);

        if (markViewed && markViewedAfterResumeReadIfNeeded(companyUserId, application)) {
            application = jobApplicationMapper.selectById(application.getId());
        }

        return view;
    }

    private boolean markViewedAfterResumeReadIfNeeded(Long companyUserId, JobApplication application) {
        ApplicationStatus currentStatus = resolveStoredStatus(application.getStatus());
        if (currentStatus != ApplicationStatus.SUBMITTED) {
            return false;
        }

        LocalDateTime viewedAt = LocalDateTime.now();
        int updatedRows = jobApplicationMapper.update(
                null,
                new LambdaUpdateWrapper<JobApplication>()
                        .eq(JobApplication::getId, application.getId())
                        .eq(JobApplication::getCompanyUserId, companyUserId)
                        .eq(JobApplication::getStatus, ApplicationStatus.SUBMITTED.name())
                        .set(JobApplication::getStatus, ApplicationStatus.VIEWED.name())
                        .set(JobApplication::getViewedAt, viewedAt)
        );
        if (updatedRows == 0) {
            return false;
        }

        application.setStatus(ApplicationStatus.VIEWED.name());
        application.setViewedAt(viewedAt);
        createStatusLog(application.getId(), ApplicationStatus.SUBMITTED.name(), ApplicationStatus.VIEWED.name(),
                companyUserId, COMPANY_OPERATOR_ROLE, null);
        notificationService.createAndPush(
                application.getJobseekerUserId(),
                NotificationType.APPLICATION_STATUS,
                "投递状态已更新",
                "您投递的岗位已被企业查看。",
                null,
                null,
                application.getId());
        return true;
    }

    private void validateCompanyRequestedStatus(ApplicationStatus newStatus) {
        if (newStatus != ApplicationStatus.REJECTED
                && newStatus != ApplicationStatus.INTERVIEW_PENDING
                && newStatus != ApplicationStatus.OFFERED) {
            throw new BusinessException("企业端不支持该状态操作");
        }
    }

    private JobApplication getCompanyApplicationOrThrow(Long companyUserId, Long applicationId, boolean forUpdate) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getId, applicationId);
        if (forUpdate) {
            wrapper.last("FOR UPDATE");
        }
        JobApplication application = jobApplicationMapper.selectOne(wrapper);
        if (application == null || !application.getCompanyUserId().equals(companyUserId)) {
            throw new BusinessException("投递记录不存在");
        }
        return application;
    }

    private JobApplication getJobseekerApplicationOrThrow(Long jobseekerUserId, Long applicationId, boolean forUpdate) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getId, applicationId);
        if (forUpdate) {
            wrapper.last("FOR UPDATE");
        }
        JobApplication application = jobApplicationMapper.selectOne(wrapper);
        if (application == null || !application.getJobseekerUserId().equals(jobseekerUserId)) {
            throw new BusinessException("投递记录不存在");
        }
        return application;
    }

    private boolean applyStatusChangeIfNeeded(JobApplication application, ApplicationStatus newStatus, Long operatorUserId,
                                              String operatorRole, String remark, StatusChangeOptions options) {
        ApplicationStatus oldStatus = resolveStoredStatus(application.getStatus());
        if (oldStatus == newStatus) {
            return false;
        }
        if (!isTransitionAllowed(oldStatus, newStatus)) {
            throw new BusinessException("当前状态不允许更新为目标状态");
        }

        LocalDateTime nextViewedAt = application.getViewedAt();
        if (options.updateViewedAt() && newStatus == ApplicationStatus.VIEWED && nextViewedAt == null) {
            nextViewedAt = LocalDateTime.now();
        }

        LambdaUpdateWrapper<JobApplication> updateWrapper = new LambdaUpdateWrapper<JobApplication>()
                .eq(JobApplication::getId, application.getId())
                .set(JobApplication::getStatus, newStatus.name())
                .set(JobApplication::getViewedAt, nextViewedAt);
        if (remark == null) {
            updateWrapper.setSql("status_remark = NULL");
        } else {
            updateWrapper.set(JobApplication::getStatusRemark, remark);
        }
        jobApplicationMapper.update(null, updateWrapper);

        application.setStatus(newStatus.name());
        application.setStatusRemark(remark);
        application.setViewedAt(nextViewedAt);

        if (options.createStatusLog()) {
            createStatusLog(application.getId(), oldStatus.name(), newStatus.name(), operatorUserId, operatorRole, remark);
        }
        return true;
    }

    private boolean isTransitionAllowed(ApplicationStatus fromStatus, ApplicationStatus toStatus) {
        if (fromStatus == toStatus) {
            return true;
        }
        return switch (fromStatus) {
            case SUBMITTED -> toStatus == ApplicationStatus.VIEWED;
            case VIEWED -> toStatus == ApplicationStatus.REJECTED
                    || toStatus == ApplicationStatus.INTERVIEW_PENDING;
            case INTERVIEW_PENDING -> toStatus == ApplicationStatus.VIEWED
                    || toStatus == ApplicationStatus.INTERVIEWING;
            case INTERVIEWING -> toStatus == ApplicationStatus.REJECTED
                    || toStatus == ApplicationStatus.OFFERED;
            case REJECTED, OFFERED -> false;
        };
    }

    private void notifyJobseekerOfCompanyStatusChange(JobApplication application, ApplicationStatus newStatus) {
        String content = switch (newStatus) {
            case REJECTED -> "很遗憾，企业已将您的投递状态更新为未通过。";
            case INTERVIEW_PENDING -> "企业已向您发出面试邀请，请尽快确认是否参加。";
            case OFFERED -> "恭喜，企业已将您的投递状态更新为已录用。";
            default -> "您的投递状态已更新。";
        };
        notificationService.createAndPush(
                application.getJobseekerUserId(),
                NotificationType.APPLICATION_STATUS,
                "投递状态已更新",
                content,
                null,
                null,
                application.getId());
    }

    private void notifyCompanyOfInterviewResponse(JobApplication application, InterviewResponse decision) {
        String content = decision == InterviewResponse.ACCEPT
                ? "候选人已接受面试邀请，当前流程已进入面试中。"
                : "候选人已拒绝面试邀请，当前状态已退回为企业已查看。";
        notificationService.createAndPush(
                application.getCompanyUserId(),
                NotificationType.APPLICATION_STATUS,
                "候选人已处理面试邀请",
                content,
                null,
                null,
                application.getId());
    }

    private void createStatusLog(Long applicationId, String fromStatus, String toStatus, Long operatorUserId,
                                 String operatorRole, String remark) {
        ApplicationStatusLog log = new ApplicationStatusLog();
        log.setApplicationId(applicationId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorRole(operatorRole);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        applicationStatusLogMapper.insert(log);
    }

    private void applyStatusFilter(LambdaQueryWrapper<JobApplication> wrapper, String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        ApplicationStatus normalizedStatus = normalizeStatus(status);
        if (normalizedStatus == ApplicationStatus.OFFERED) {
            wrapper.and(condition -> condition
                    .eq(JobApplication::getStatus, ApplicationStatus.OFFERED.name())
                    .or()
                    .eq(JobApplication::getStatus, LEGACY_ACCEPTED_STATUS));
            return;
        }
        wrapper.eq(JobApplication::getStatus, normalizedStatus.name());
    }

    private ApplicationStatus normalizeStatus(String status) {
        try {
            return ApplicationStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("不支持的投递状态");
        }
    }

    private InterviewResponse normalizeInterviewResponse(String decision) {
        if (decision == null || decision.isBlank()) {
            throw new BusinessException("面试确认结果不能为空");
        }
        try {
            return InterviewResponse.valueOf(decision.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("不支持的面试确认结果");
        }
    }

    private ApplicationStatus resolveStoredStatus(String status) {
        try {
            return ApplicationStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            return ApplicationStatus.SUBMITTED;
        }
    }

    private ApplicationDtos.ApplicationRecordView buildApplicationView(JobApplication application) {
        JobPost jobPost = jobPostMapper.selectById(application.getJobId());
        CompanyProfile companyProfile = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, application.getCompanyUserId()));
        ApplicationStatus applicationStatus = resolveStoredStatus(application.getStatus());

        ApplicationDtos.ApplicationRecordView view = new ApplicationDtos.ApplicationRecordView();
        view.setId(application.getId());
        view.setJobId(application.getJobId());
        view.setJobTitle(jobPost == null ? "" : jobPost.getTitle());
        view.setCompanyUserId(application.getCompanyUserId());
        view.setCompanyName(companyProfile == null ? "" : companyProfile.getCompanyName());
        view.setJobseekerUserId(application.getJobseekerUserId());
        view.setResumeId(application.getResumeId());
        view.setSavedResumeId(application.getSavedResumeId());
        view.setSavedResumeName(application.getSavedResumeName());
        view.setStatus(applicationStatus.name());
        view.setStatusText(applicationStatus.getLabel());
        view.setStatusDescription(applicationStatus.getDescription());
        view.setStatusRemark(application.getStatusRemark());
        view.setAppliedAt(application.getAppliedAt());
        view.setViewedAt(application.getViewedAt());
        view.setStatusUpdatedAt(resolveStatusUpdatedAt(application));
        return view;
    }

    private LocalDateTime resolveStatusUpdatedAt(JobApplication application) {
        ApplicationStatusLog latestLog = applicationStatusLogMapper.selectOne(new LambdaQueryWrapper<ApplicationStatusLog>()
                .eq(ApplicationStatusLog::getApplicationId, application.getId())
                .orderByDesc(ApplicationStatusLog::getCreatedAt)
                .last("LIMIT 1"));
        if (latestLog != null && latestLog.getCreatedAt() != null) {
            return latestLog.getCreatedAt();
        }
        if (application.getUpdatedAt() != null) {
            return application.getUpdatedAt();
        }
        return application.getAppliedAt();
    }

    private AdminDtos.AdminApplicationView buildAdminApplicationView(JobApplication application,
                                                                     JobPost jobPost,
                                                                     String companyName,
                                                                     String jobseekerName) {
        ApplicationStatus applicationStatus = resolveStoredStatus(application.getStatus());
        AdminDtos.AdminApplicationView view = new AdminDtos.AdminApplicationView();
        view.setId(application.getId());
        view.setJobId(application.getJobId());
        view.setJobTitle(jobPost == null ? "" : jobPost.getTitle());
        view.setCompanyUserId(application.getCompanyUserId());
        view.setCompanyName(companyName == null ? "" : companyName);
        view.setJobseekerUserId(application.getJobseekerUserId());
        view.setJobseekerName(jobseekerName);
        view.setResumeId(application.getResumeId());
        view.setStatus(applicationStatus.name());
        view.setStatusText(applicationStatus.getLabel());
        view.setStatusDescription(applicationStatus.getDescription());
        view.setStatusRemark(application.getStatusRemark());
        view.setAppliedAt(application.getAppliedAt());
        view.setStatusUpdatedAt(resolveStatusUpdatedAt(application));
        return view;
    }

    private String resolveJobseekerName(Long jobseekerUserId,
                                        Map<Long, JobseekerProfile> jobseekerProfileMap,
                                        Map<Long, SysUser> userMap) {
        JobseekerProfile profile = jobseekerProfileMap.get(jobseekerUserId);
        if (profile != null && StringUtils.hasText(profile.getFullName())) {
            return profile.getFullName();
        }
        SysUser user = userMap.get(jobseekerUserId);
        return user == null ? "" : user.getDisplayName();
    }

    private boolean matchesAdminKeyword(String keyword, AdminDtos.AdminApplicationView view) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return safe(view.getJobTitle()).toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || safe(view.getCompanyName()).toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || safe(view.getJobseekerName()).toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private boolean matchesAdminStatus(String status, String actualStatus) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return normalizeStatus(status).name().equals(actualStatus);
    }

    private boolean matchesAppliedAtRange(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime appliedAt) {
        if (appliedAt == null) {
            return startAt == null && endAt == null;
        }
        if (startAt != null && appliedAt.isBefore(startAt)) {
            return false;
        }
        return endAt == null || !appliedAt.isAfter(endAt);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private <T> PageResponse<T> paginate(List<T> records, long pageNum, long pageSize) {
        long current = pageNum < 1 ? 1 : pageNum;
        long size = pageSize < 1 ? 10 : pageSize;
        int fromIndex = (int) ((current - 1) * size);
        if (fromIndex >= records.size()) {
            return PageResponse.<T>builder()
                    .pageNum(current)
                    .pageSize(size)
                    .total(records.size())
                    .records(List.of())
                    .build();
        }
        int toIndex = Math.min(records.size(), fromIndex + (int) size);
        return PageResponse.<T>builder()
                .pageNum(current)
                .pageSize(size)
                .total(records.size())
                .records(records.subList(fromIndex, toIndex))
                .build();
    }

    private record StatusChangeOptions(boolean createStatusLog, boolean updateViewedAt) {
        private static StatusChangeOptions defaultOptions() {
            return new StatusChangeOptions(true, true);
        }
    }

    private enum InterviewResponse {
        ACCEPT,
        REJECT
    }
}
