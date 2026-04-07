package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.AuthDtos;
import com.bishe.recruitment.service.AuthService;
import com.bishe.recruitment.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/company/register")
    public ApiResponse<?> registerCompany(@Valid @RequestBody AuthDtos.CompanyRegisterRequest request) {
        return ApiResponse.success("企业注册成功", authService.registerCompany(request));
    }

    @PostMapping("/jobseeker/register")
    public ApiResponse<?> registerJobseeker(@Valid @RequestBody AuthDtos.JobseekerRegisterRequest request) {
        return ApiResponse.success("求职者注册成功", authService.registerJobseeker(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.success(authService.currentProfile(SecurityUtils.currentUserId()));
    }
}
