package com.bishe.recruitment.enums;

import java.util.Locale;

public enum ApplicationStatus {
    SUBMITTED("已投递", "等待企业查看"),
    VIEWED("企业已查看", "企业已查看该简历"),
    INTERVIEW_PENDING("待面试确认", "等待候选人确认面试安排"),
    REJECTED("未通过", "该候选人未通过当前筛选"),
    INTERVIEWING("面试中", "候选人已进入面试流程"),
    OFFERED("已录用", "候选人已被录用");

    private final String label;
    private final String description;

    ApplicationStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static ApplicationStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Application status cannot be blank");
        }
        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        if ("ACCEPTED".equals(normalizedValue)) {
            return OFFERED;
        }
        return ApplicationStatus.valueOf(normalizedValue);
    }
}
