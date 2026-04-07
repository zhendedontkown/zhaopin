package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.ApplicationDtos;
import com.bishe.recruitment.entity.ApplicationStatusLog;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobApplication;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.enums.ApplicationStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.mapper.ApplicationStatusLogMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobApplicationMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final JobApplicationMapper jobApplicationMapper;
    private final ApplicationStatusLogMapper applicationStatusLogMapper;
    private final JobPostMapper jobPostMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final ResumeService resumeService;
    private final NotificationService notificationService;

    public ApplicationService(JobApplicationMapper jobApplicationMapper, ApplicationStatusLogMapper applicationStatusLogMapper,
                              JobPostMapper jobPostMapper, CompanyProfileMapper companyProfileMapper,
                              ResumeService resumeService, NotificationService notificationService) {
        this.jobApplicationMapper = jobApplicationMapper;
        this.applicationStatusLogMapper = applicationStatusLogMapper;
        this.jobPostMapper = jobPostMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.resumeService = resumeService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Map<String, Object> apply(Long jobseekerUserId, ApplicationDtos.ApplyJobRequest request) {
        Resume resume = resumeService.validateCompleteOrThrow(jobseekerUserId);
        if (request.getResumeId() != null && !request.getResumeId().equals(resume.getId())) {
            throw new BusinessException("当前版本仅支持投递主简历");
        }
        JobPost jobPost = jobPostMapper.selectOne(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getId, request.getJobId())
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
        application.setResumeId(resume.getId());
        application.setStatus(ApplicationStatus.SUBMITTED.name());
        application.setAppliedAt(LocalDateTime.now());
        jobApplicationMapper.insert(application);
        createStatusLog(application.getId(), null, ApplicationStatus.SUBMITTED.name(), jobseekerUserId, "JOBSEEKER", null);
        notificationService.createAndPush(jobPost.getCompanyUserId(), NotificationType.NEW_APPLICATION,
                "收到新的简历投递", "岗位 " + jobPost.getTitle() + " 收到了一份新的简历投递。");
        return buildApplicationView(application);
    }

    public PageResponse<Map<String, Object>> listByJobseeker(Long jobseekerUserId, long pageNum, long pageSize) {
        Page<JobApplication> page = new Page<>(pageNum, pageSize);
        Page<JobApplication> result = jobApplicationMapper.selectPage(page, new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getJobseekerUserId, jobseekerUserId)
                .orderByDesc(JobApplication::getAppliedAt));
        List<Map<String, Object>> records = result.getRecords().stream().map(this::buildApplicationView).toList();
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    public PageResponse<Map<String, Object>> listByCompany(Long companyUserId, String status, long pageNum, long pageSize) {
        Page<JobApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getCompanyUserId, companyUserId)
                .orderByDesc(JobApplication::getAppliedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(JobApplication::getStatus, status);
        }
        Page<JobApplication> result = jobApplicationMapper.selectPage(page, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(this::buildApplicationView).toList();
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    @Transactional
    public Map<String, Object> updateStatus(Long companyUserId, Long applicationId, ApplicationDtos.UpdateApplicationStatusRequest request) {
        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(request.getStatus());
        } catch (Exception ex) {
            throw new BusinessException("不支持的投递状态");
        }
        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null || !application.getCompanyUserId().equals(companyUserId)) {
            throw new BusinessException("投递记录不存在");
        }
        String oldStatus = application.getStatus();
        application.setStatus(newStatus.name());
        application.setStatusRemark(request.getRemark());
        if (newStatus == ApplicationStatus.VIEWED) {
            application.setViewedAt(LocalDateTime.now());
        }
        jobApplicationMapper.updateById(application);
        createStatusLog(application.getId(), oldStatus, newStatus.name(), companyUserId, "COMPANY", request.getRemark());
        notificationService.createAndPush(application.getJobseekerUserId(), NotificationType.APPLICATION_STATUS,
                "投递状态已更新", "您的简历投递状态已更新为 " + newStatus.name());
        return buildApplicationView(application);
    }

    public boolean existsApplicationBetween(Long companyUserId, Long jobseekerUserId) {
        return jobApplicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getCompanyUserId, companyUserId)
                .eq(JobApplication::getJobseekerUserId, jobseekerUserId)) > 0;
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

    private Map<String, Object> buildApplicationView(JobApplication application) {
        JobPost jobPost = jobPostMapper.selectById(application.getJobId());
        CompanyProfile companyProfile = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, application.getCompanyUserId()));
        LinkedHashMap<String, Object> view = new LinkedHashMap<>();
        view.put("id", application.getId());
        view.put("jobId", application.getJobId());
        view.put("jobTitle", jobPost == null ? "" : jobPost.getTitle());
        view.put("companyUserId", application.getCompanyUserId());
        view.put("companyName", companyProfile == null ? "" : companyProfile.getCompanyName());
        view.put("jobseekerUserId", application.getJobseekerUserId());
        view.put("resumeId", application.getResumeId());
        view.put("status", application.getStatus());
        view.put("statusRemark", application.getStatusRemark());
        view.put("appliedAt", application.getAppliedAt());
        view.put("viewedAt", application.getViewedAt());
        return view;
    }
}
