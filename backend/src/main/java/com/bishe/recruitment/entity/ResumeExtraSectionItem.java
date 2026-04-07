package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("resume_extra_section_item")
public class ResumeExtraSectionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private String sectionCode;

    private String title;

    private String subtitle;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean currentFlag;

    private String description;

    private Integer sortOrder;
}
