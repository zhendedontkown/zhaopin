package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.ApplicationDtos;
import com.bishe.recruitment.dto.AuthDtos;
import com.bishe.recruitment.dto.ResumeDtos;
import com.bishe.recruitment.service.ApplicationService;
import com.bishe.recruitment.service.AuthService;
import com.bishe.recruitment.service.JobFavoriteService;
import com.bishe.recruitment.service.ResumeService;
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
@RequestMapping("/api/jobseeker")
public class JobseekerController {

    private final ResumeService resumeService;
    private final ApplicationService applicationService;
    private final AuthService authService;
    private final JobFavoriteService jobFavoriteService;

    public JobseekerController(ResumeService resumeService, ApplicationService applicationService,
                               AuthService authService, JobFavoriteService jobFavoriteService) {
        this.resumeService = resumeService;
        this.applicationService = applicationService;
        this.authService = authService;
        this.jobFavoriteService = jobFavoriteService;
    }

    @GetMapping("/resume")
    public ApiResponse<?> getResume() {
        return ApiResponse.success(resumeService.getResumeDetail(SecurityUtils.currentUserId()));
    }

    @PutMapping("/resume")
    public ApiResponse<?> saveResume(@Valid @RequestBody ResumeDtos.ResumeSaveRequest request) {
        return ApiResponse.success("简历保存成功", resumeService.saveResume(SecurityUtils.currentUserId(), request));
    }

    @PostMapping("/saved-resumes")
    public ApiResponse<?> createSavedResume(@Valid @RequestBody ResumeDtos.CreateSavedResumeRequest request) {
        return ApiResponse.success("简历保存成功", resumeService.createSavedResume(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/saved-resumes")
    public ApiResponse<?> listSavedResumes(@RequestParam(defaultValue = "false") boolean completeOnly,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(resumeService.listSavedResumes(SecurityUtils.currentUserId(), completeOnly, pageNum, pageSize));
    }

    @GetMapping("/saved-resumes/{savedResumeId}")
    public ApiResponse<?> getSavedResumeDetail(@PathVariable Long savedResumeId) {
        return ApiResponse.success(resumeService.getSavedResumeDetail(SecurityUtils.currentUserId(), savedResumeId));
    }

    @PutMapping("/saved-resumes/{savedResumeId}")
    public ApiResponse<?> updateSavedResume(@PathVariable Long savedResumeId,
                                            @Valid @RequestBody ResumeDtos.UpdateSavedResumeRequest request) {
        return ApiResponse.success("简历更新成功", resumeService.updateSavedResume(SecurityUtils.currentUserId(), savedResumeId, request));
    }

    @DeleteMapping("/saved-resumes/{savedResumeId}")
    public ApiResponse<?> deleteSavedResume(@PathVariable Long savedResumeId) {
        resumeService.deleteSavedResume(SecurityUtils.currentUserId(), savedResumeId);
        return ApiResponse.success("已删除保存简历");
    }

    @PostMapping("/applications")
    public ApiResponse<?> applyJob(@Valid @RequestBody ApplicationDtos.ApplyJobRequest request) {
        return ApiResponse.success("投递成功", applicationService.apply(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/applications")
    public ApiResponse<?> listMyApplications(@RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(applicationService.listByJobseeker(SecurityUtils.currentUserId(), status, pageNum, pageSize));
    }

    @PatchMapping("/applications/{applicationId}/interview-response")
    public ApiResponse<?> respondToInterviewInvitation(@PathVariable Long applicationId,
                                                       @Valid @RequestBody ApplicationDtos.InterviewResponseRequest request) {
        return ApiResponse.success(
                "面试邀请处理成功",
                applicationService.respondToInterviewInvitation(SecurityUtils.currentUserId(), applicationId, request));
    }

    @PostMapping("/favorites/{jobId}")
    public ApiResponse<?> favoriteJob(@PathVariable Long jobId) {
        return ApiResponse.success("岗位已加入收藏", jobFavoriteService.favoriteJob(SecurityUtils.currentUserId(), jobId));
    }

    @DeleteMapping("/favorites/{jobId}")
    public ApiResponse<?> unfavoriteJob(@PathVariable Long jobId) {
        return ApiResponse.success("岗位已取消收藏", jobFavoriteService.unfavoriteJob(SecurityUtils.currentUserId(), jobId));
    }

    @GetMapping("/favorites")
    public ApiResponse<?> listFavorites(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "5") long pageSize) {
        return ApiResponse.success(jobFavoriteService.listFavorites(SecurityUtils.currentUserId(), pageNum, pageSize));
    }

    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(@Valid @RequestBody AuthDtos.JobseekerProfileUpdateRequest request) {
        return ApiResponse.success("求职者资料已更新", authService.updateJobseekerProfile(SecurityUtils.currentUserId(), request));
    }
}
