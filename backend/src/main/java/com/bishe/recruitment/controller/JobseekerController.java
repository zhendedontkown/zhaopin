package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.ApplicationDtos;
import com.bishe.recruitment.dto.ResumeDtos;
import com.bishe.recruitment.service.ApplicationService;
import com.bishe.recruitment.service.ResumeService;
import com.bishe.recruitment.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobseeker")
public class JobseekerController {

    private final ResumeService resumeService;
    private final ApplicationService applicationService;

    public JobseekerController(ResumeService resumeService, ApplicationService applicationService) {
        this.resumeService = resumeService;
        this.applicationService = applicationService;
    }

    @GetMapping("/resume")
    public ApiResponse<?> getResume() {
        return ApiResponse.success(resumeService.getResumeDetail(SecurityUtils.currentUserId()));
    }

    @PutMapping("/resume")
    public ApiResponse<?> saveResume(@Valid @RequestBody ResumeDtos.ResumeSaveRequest request) {
        return ApiResponse.success("简历保存成功", resumeService.saveResume(SecurityUtils.currentUserId(), request));
    }

    @PostMapping("/applications")
    public ApiResponse<?> applyJob(@Valid @RequestBody ApplicationDtos.ApplyJobRequest request) {
        return ApiResponse.success("投递成功", applicationService.apply(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/applications")
    public ApiResponse<?> listMyApplications(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(applicationService.listByJobseeker(SecurityUtils.currentUserId(), pageNum, pageSize));
    }
}
