package com.bishe.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public final class AdminDtos {

    private AdminDtos() {
    }

    @Data
    public static class CompanyAuditRequest {
        @NotBlank(message = "审核状态不能为空")
        private String auditStatus;
    }

    @Data
    public static class JobStatusRequest {
        @NotBlank(message = "岗位状态不能为空")
        private String status;
    }
}
