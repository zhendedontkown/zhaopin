package com.bishe.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class ResumeDtos {

    private ResumeDtos() {
    }

    @Data
    public static class ResumeSaveRequest {
        @NotBlank(message = "模板不能为空")
        private String templateCode;
        private String fullName;
        private String gender;
        private Integer age;
        private LocalDate birthDate;
        private Boolean displayAge;
        private String phone;
        private String email;
        private String city;
        private String summary;
        private String expectedCategory;
        private Integer expectedSalaryMin;
        private Integer expectedSalaryMax;
        private String highestEducation;
        private Integer yearsOfExperience;
        private List<ModuleConfigItem> moduleConfig = new ArrayList<>();
        private List<EducationItem> educations = new ArrayList<>();
        private List<ExperienceItem> experiences = new ArrayList<>();
        private List<ProjectItem> projects = new ArrayList<>();
        private List<ExtraSectionItem> internships = new ArrayList<>();
        private List<ExtraSectionItem> campusExperiences = new ArrayList<>();
        private List<ExtraSectionItem> honors = new ArrayList<>();
        private List<ExtraSectionItem> hobbies = new ArrayList<>();
        private List<CustomFieldItem> customFields = new ArrayList<>();
        private List<String> skills = new ArrayList<>();
    }

    @Data
    public static class ModuleConfigItem {
        private String code;
        private String label;
        private Boolean visible;
        private Integer order;
    }

    @Data
    public static class EducationItem {
        private String schoolName;
        private String major;
        private String degree;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
        private String description;
        private Integer sortOrder;
    }

    @Data
    public static class ExperienceItem {
        private String companyName;
        private String jobTitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
        private String description;
        private Integer sortOrder;
    }

    @Data
    public static class ProjectItem {
        private String projectName;
        private String roleName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
        private String description;
        private Integer sortOrder;
    }

    @Data
    public static class ExtraSectionItem {
        private Long id;
        private String title;
        private String subtitle;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
        private String description;
        private Integer sortOrder;
    }

    @Data
    public static class CustomFieldItem {
        private String key;
        private String value;
        private Integer sortOrder;
    }
}
