package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resume")
public class Resume extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

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

    private String moduleConfigJson;

    private String expectedCategory;

    private Integer expectedSalaryMin;

    private Integer expectedSalaryMax;

    private String highestEducation;

    private Integer yearsOfExperience;

    private Integer completenessScore;
}
