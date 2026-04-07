package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("resume_education")
public class ResumeEducation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private String schoolName;

    private String major;

    private String degree;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean currentFlag;

    private String description;

    private Integer sortOrder;
}
