package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.service.ResumeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final ResumeService resumeService;

    public SkillController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/suggest")
    public ApiResponse<?> suggest(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(resumeService.suggestSkills(keyword));
    }
}
