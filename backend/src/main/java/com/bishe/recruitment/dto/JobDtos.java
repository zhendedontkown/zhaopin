package com.bishe.recruitment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

public final class JobDtos {

    private JobDtos() {
    }

    @Data
    public static class JobSaveRequest {
        @NotBlank(message = "岗位名称不能为空")
        private String title;

        @NotBlank(message = "岗位类别不能为空")
        private String category;

        @NotBlank(message = "工作地点不能为空")
        private String location;

        @NotNull(message = "最低薪资不能为空")
        @Min(value = 0, message = "最低薪资不能小于0")
        private Integer salaryMin;

        @NotNull(message = "最高薪资不能为空")
        @Min(value = 0, message = "最高薪资不能小于0")
        private Integer salaryMax;

        @NotBlank(message = "经验要求不能为空")
        private String experienceRequirement;

        @NotBlank(message = "学历要求不能为空")
        private String educationRequirement;

        @NotNull(message = "招聘人数不能为空")
        @Min(value = 1, message = "招聘人数至少为1")
        @Max(value = 999, message = "招聘人数不能超过999")
        private Integer headcount;

        @NotBlank(message = "岗位描述不能为空")
        private String description;

        @Future(message = "过期时间必须晚于当前时间")
        private LocalDateTime expireAt;
    }

    @Data
    public static class JobSearchRequest {
        private String keyword;
        private String category;
        private String location;
        private Integer salaryMin;
        private Integer salaryMax;
        private String educationRequirement;
        private String experienceRequirement;
        private String sortBy;
        private String sortDirection;
        private Long pageNum = 1L;
        private Long pageSize = 10L;
    }
}
