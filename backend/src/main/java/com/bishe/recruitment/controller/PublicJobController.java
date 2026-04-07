package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.JobDtos;
import com.bishe.recruitment.service.JobService;
import com.bishe.recruitment.security.AuthenticatedUser;
import com.bishe.recruitment.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class PublicJobController {

    private final JobService jobService;

    public PublicJobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@ModelAttribute JobDtos.JobSearchRequest request, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(jobService.searchJobs(request, userId));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<?> detail(@PathVariable Long jobId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(jobService.getJobDetail(jobId, userId));
    }

    @GetMapping("/recommend")
    public ApiResponse<?> recommend() {
        return ApiResponse.success(jobService.recommendJobs(SecurityUtils.currentUserId()));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user.getUserId();
    }
}
