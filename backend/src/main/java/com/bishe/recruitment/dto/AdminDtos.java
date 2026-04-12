package com.bishe.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Data;

public final class AdminDtos {

    private AdminDtos() {
    }

    @Data
    public static class CompanyAuditRequest {
        @NotBlank(message = "审核状态不能为空")
        private String auditStatus;

        private String reason;
    }

    @Data
    public static class JobStatusRequest {
        @NotBlank(message = "岗位状态不能为空")
        private String status;
    }

    @Data
    public static class UserStatusRequest {
        @NotBlank(message = "账号状态不能为空")
        private String status;

        private String reason;
    }

    @Data
    public static class JobModerationRequest {
        @NotBlank(message = "处理动作不能为空")
        private String action;

        private String reason;
    }

    @Data
    public static class DashboardView {
        private long userCount;
        private long companyUserCount;
        private long jobseekerUserCount;
        private long pendingCompanyAuditCount;
        private long jobCount;
        private long applicationCount;
        private long interviewingCount;
        private long offeredCount;
        private long rejectedCount;
    }

    @Data
    public static class CompanyUserView {
        private Long userId;
        private String displayName;
        private String companyName;
        private String contactPerson;
        private String phone;
        private String email;
        private String unifiedSocialCreditCode;
        private String auditStatus;
        private String userStatus;
        private LocalDateTime createdAt;
    }

    @Data
    public static class JobseekerUserView {
        private Long userId;
        private String displayName;
        private String fullName;
        private String phone;
        private String email;
        private String highestEducation;
        private String desiredPositionCategory;
        private String userStatus;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ManagedJobView {
        private Long id;
        private String jobCode;
        private String title;
        private String category;
        private String location;
        private Integer salaryMin;
        private Integer salaryMax;
        private String experienceRequirement;
        private String educationRequirement;
        private String status;
        private LocalDateTime publishedAt;
        private LocalDateTime createdAt;
        private Long companyUserId;
        private String companyName;
    }

    @Data
    public static class AdminApplicationView {
        private Long id;
        private Long jobId;
        private String jobTitle;
        private Long companyUserId;
        private String companyName;
        private Long jobseekerUserId;
        private String jobseekerName;
        private Long resumeId;
        private String status;
        private String statusText;
        private String statusDescription;
        private String statusRemark;
        private LocalDateTime appliedAt;
        private LocalDateTime statusUpdatedAt;
    }
}
