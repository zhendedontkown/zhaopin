package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job_application")
public class JobApplication extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private Long companyUserId;

    private Long jobseekerUserId;

    private Long resumeId;

    private String status;

    private String statusRemark;

    private LocalDateTime appliedAt;

    private LocalDateTime viewedAt;
}
