package com.bishe.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    @Data
    public static class ApplyJobRequest {
        @NotNull(message = "岗位ID不能为空")
        private Long jobId;

        private Long resumeId;

        private Long savedResumeId;
    }

    @Data
    public static class UpdateApplicationStatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;

        private String remark;
    }

    @Data
    public static class InterviewResponseRequest {
        @NotBlank(message = "面试确认结果不能为空")
        private String decision;
    }

    @Data
    public static class ApplicationRecordView {
        private Long id;
        private Long jobId;
        private String jobTitle;
        private Long companyUserId;
        private String companyName;
        private Long jobseekerUserId;
        private Long resumeId;
        private Long savedResumeId;
        private String savedResumeName;
        private String status;
        private String statusText;
        private String statusDescription;
        private String statusRemark;
        private LocalDateTime appliedAt;
        private LocalDateTime viewedAt;
        private LocalDateTime statusUpdatedAt;
    }

    @Data
    public static class ApplicationResumeView {
        private Long applicationId;
        private Long resumeId;
        private Long savedResumeId;
        private String savedResumeName;
        private String jobTitle;
        private String companyName;
        private Boolean snapshotBased;
        private String resumeSource;
        private Map<String, Object> resumeDetail;
    }
}
