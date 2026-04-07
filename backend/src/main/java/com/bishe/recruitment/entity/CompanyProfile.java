package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_profile")
public class CompanyProfile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String companyName;

    private String unifiedSocialCreditCode;

    private String contactPerson;

    private String phone;

    private String email;

    private String address;

    private String description;

    private String auditStatus;
}
