package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.AdminDtos;
import com.bishe.recruitment.service.AdminService;
import com.bishe.recruitment.service.JobService;
import jakarta.validation.Valid;
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

    public AdminController(AdminService adminService, JobService jobService) {
        this.adminService = adminService;
        this.jobService = jobService;
    }

    @GetMapping("/companies")
    public ApiResponse<?> listCompanies(@RequestParam(required = false) String auditStatus,
                                        @RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(adminService.listCompanies(auditStatus, pageNum, pageSize));
    }

    @PatchMapping("/companies/{userId}/audit")
    public ApiResponse<?> auditCompany(@PathVariable Long userId, @Valid @RequestBody AdminDtos.CompanyAuditRequest request) {
        return ApiResponse.success("审核完成", adminService.auditCompany(userId, request.getAuditStatus()));
    }

    @GetMapping("/jobs")
    public ApiResponse<?> listJobs(@RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") long pageNum,
                                   @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(jobService.listJobsForAdmin(status, pageNum, pageSize));
    }

    @PatchMapping("/jobs/{jobId}/status")
    public ApiResponse<?> updateJobStatus(@PathVariable Long jobId, @Valid @RequestBody AdminDtos.JobStatusRequest request) {
        return ApiResponse.success("岗位状态已更新", jobService.updateJobStatusByAdmin(jobId, request.getStatus()));
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }
}
