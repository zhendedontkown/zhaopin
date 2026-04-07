package com.bishe.recruitment.util;

import com.bishe.recruitment.dto.ResumeDtos;
import java.util.List;

public final class ResumeModuleConfigSupport {

    public static final String BASIC_INFO = "basicInfo";
    public static final String JOB_INTENT = "jobIntent";
    public static final String EDUCATION = "education";
    public static final String WORK_EXPERIENCE = "workExperience";
    public static final String PROJECT_EXPERIENCE = "projectExperience";
    public static final String INTERNSHIP = "internshipExperience";
    public static final String CAMPUS = "campusExperience";
    public static final String SKILLS = "skills";
    public static final String HONORS = "honors";
    public static final String SELF_EVALUATION = "selfEvaluation";
    public static final String HOBBIES = "hobbies";
    public static final String CUSTOM_FIELDS = "customFields";

    private ResumeModuleConfigSupport() {
    }

    public static List<ResumeDtos.ModuleConfigItem> defaultModuleConfig() {
        return List.of(
                item(BASIC_INFO, "基本信息", true, 0),
                item(JOB_INTENT, "求职意向", true, 1),
                item(EDUCATION, "教育背景", true, 2),
                item(WORK_EXPERIENCE, "工作经历", true, 3),
                item(PROJECT_EXPERIENCE, "项目经历", true, 4),
                item(INTERNSHIP, "实习经历", false, 5),
                item(CAMPUS, "校园经历", false, 6),
                item(SKILLS, "技能特长", true, 7),
                item(HONORS, "荣誉证书", false, 8),
                item(SELF_EVALUATION, "自我评价", true, 9),
                item(HOBBIES, "兴趣爱好", false, 10),
                item(CUSTOM_FIELDS, "自定义信息", false, 11)
        );
    }

    private static ResumeDtos.ModuleConfigItem item(String code, String label, boolean visible, int order) {
        ResumeDtos.ModuleConfigItem item = new ResumeDtos.ModuleConfigItem();
        item.setCode(code);
        item.setLabel(label);
        item.setVisible(visible);
        item.setOrder(order);
        return item;
    }
}
