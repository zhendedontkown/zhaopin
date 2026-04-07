package com.bishe.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    @Data
    public static class ApplyJobRequest {
        @NotNull(message = "岗位ID不能为空")
        private Long jobId;

        private Long resumeId;
    }

    @Data
    public static class UpdateApplicationStatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;

        private String remark;
    }
}
