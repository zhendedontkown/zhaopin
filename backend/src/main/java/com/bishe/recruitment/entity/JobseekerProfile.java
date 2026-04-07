package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("jobseeker_profile")
public class JobseekerProfile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String fullName;

    private String phone;

    private String email;

    private String desiredPositionCategory;

    private Integer expectedSalaryMin;

    private Integer expectedSalaryMax;

    private String preferredCity;

    private String highestEducation;

    private Integer yearsOfExperience;
}
