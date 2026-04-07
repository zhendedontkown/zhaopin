package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.AdminDtos;
import com.bishe.recruitment.dto.ApplicationDtos;
import com.bishe.recruitment.dto.JobDtos;
import com.bishe.recruitment.service.ApplicationService;
import com.bishe.recruitment.service.JobService;
import com.bishe.recruitment.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    public CompanyController(JobService jobService, ApplicationService applicationService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
    }

    @PostMapping("/jobs")
    public ApiResponse<?> createJob(@Valid @RequestBody JobDtos.JobSaveRequest request) {
        return ApiResponse.success("岗位创建成功", jobService.createJob(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/jobs/{jobId}")
    public ApiResponse<?> updateJob(@PathVariable Long jobId, @Valid @RequestBody JobDtos.JobSaveRequest request) {
        return ApiResponse.success("岗位更新成功", jobService.updateJob(SecurityUtils.currentUserId(), jobId, request));
    }

    @DeleteMapping("/jobs/{jobId}")
    public ApiResponse<?> deleteJob(@PathVariable Long jobId) {
        jobService.deleteJob(SecurityUtils.currentUserId(), jobId);
        return ApiResponse.success("岗位删除成功", null);
    }

    @PatchMapping("/jobs/{jobId}/status")
    public ApiResponse<?> updateJobStatus(@PathVariable Long jobId, @Valid @RequestBody AdminDtos.JobStatusRequest request) {
        return ApiResponse.success("岗位状态已更新", jobService.updateJobStatusByCompany(SecurityUtils.currentUserId(), jobId, request.getStatus()));
    }

    @GetMapping("/jobs")
    public ApiResponse<?> listJobs(@RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") long pageNum,
                                   @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(jobService.listCompanyJobs(SecurityUtils.currentUserId(), status, pageNum, pageSize));
    }

    @GetMapping("/applications")
    public ApiResponse<?> listApplications(@RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(applicationService.listByCompany(SecurityUtils.currentUserId(), status, pageNum, pageSize));
    }

    @PatchMapping("/applications/{applicationId}/status")
    public ApiResponse<?> updateApplicationStatus(@PathVariable Long applicationId,
                                                  @Valid @RequestBody ApplicationDtos.UpdateApplicationStatusRequest request) {
        return ApiResponse.success("投递状态已更新",
                applicationService.updateStatus(SecurityUtils.currentUserId(), applicationId, request));
    }
}
