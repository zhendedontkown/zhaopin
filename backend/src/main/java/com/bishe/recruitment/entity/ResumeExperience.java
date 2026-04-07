package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("resume_experience")
public class ResumeExperience {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private String companyName;

    private String jobTitle;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean currentFlag;

    private String description;

    private Integer sortOrder;
}
