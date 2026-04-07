package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("application_status_log")
public class ApplicationStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private String fromStatus;

    private String toStatus;

    private Long operatorUserId;

    private String operatorRole;

    private String remark;

    private LocalDateTime createdAt;
}
