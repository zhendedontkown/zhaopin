package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.AdminDtos;
import com.bishe.recruitment.service.AdminService;
import com.bishe.recruitment.service.ApplicationService;
import com.bishe.recruitment.service.JobService;
import com.bishe.recruitment.util.SecurityUtils;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final JobService jobService;
    private final ApplicationService applicationService;

    public AdminController(AdminService adminService, JobService jobService, ApplicationService applicationService) {
        this.adminService = adminService;
        this.jobService = jobService;
        this.applicationService = applicationService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    @GetMapping("/users/companies")
    public ApiResponse<?> listCompanyUsers(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String auditStatus,
                                           @RequestParam(required = false) String userStatus,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(adminService.listCompanyUsers(keyword, auditStatus, userStatus, pageNum, pageSize));
    }

    @GetMapping("/users/jobseekers")
    public ApiResponse<?> listJobseekers(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String userStatus,
                                         @RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(adminService.listJobseekers(keyword, userStatus, pageNum, pageSize));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<?> updateUserStatus(@PathVariable Long userId,
                                           @Valid @RequestBody AdminDtos.UserStatusRequest request) {
        return ApiResponse.success(
                "账号状态已更新",
                adminService.updateUserStatus(userId, request.getStatus(), request.getReason(), SecurityUtils.currentUserId()));
    }

    @GetMapping("/company-audits")
    public ApiResponse<?> listCompanyAudits(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String auditStatus,
                                            @RequestParam(required = false) String userStatus,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(adminService.listCompanyAudits(keyword, auditStatus, userStatus, pageNum, pageSize));
    }

    @PatchMapping("/companies/{userId}/audit")
    public ApiResponse<?> auditCompany(@PathVariable Long userId,
                                       @Valid @RequestBody AdminDtos.CompanyAuditRequest request) {
        return ApiResponse.success(
                "审核完成",
                adminService.auditCompany(userId, request.getAuditStatus(), request.getReason(), SecurityUtils.currentUserId()));
    }

    @GetMapping("/jobs")
    public ApiResponse<?> listJobs(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String companyKeyword,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(defaultValue = "1") long pageNum,
                                   @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(jobService.listJobsForAdmin(keyword, companyKeyword, status, category, pageNum, pageSize));
    }

    @PatchMapping("/jobs/{jobId}/moderation")
    public ApiResponse<?> moderateJob(@PathVariable Long jobId,
                                      @Valid @RequestBody AdminDtos.JobModerationRequest request) {
        return ApiResponse.success(
                "岗位处理完成",
                jobService.moderateJobByAdmin(SecurityUtils.currentUserId(), jobId, request.getAction(), request.getReason()));
    }

    @GetMapping("/applications")
    public ApiResponse<?> listApplications(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(applicationService.listForAdmin(keyword, status, startAt, endAt, pageNum, pageSize));
    }
}
