package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job_post")
public class JobPost extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long companyUserId;

    private String jobCode;

    private String title;

    private String category;

    private String location;

    private Integer salaryMin;

    private Integer salaryMax;

    private String experienceRequirement;

    private String educationRequirement;

    private Integer headcount;

    private String description;

    private String benefitTagsJson;

    private String skillTagsJson;

    private String status;

    private LocalDateTime publishedAt;

    private LocalDateTime expireAt;

    private Integer deletedFlag;

    private LocalDateTime deletedAt;

    private Long deletedBy;

    @TableField(exist = false)
    private List<String> skillTags;

    @TableField(exist = false)
    private List<String> benefitTags;
}
