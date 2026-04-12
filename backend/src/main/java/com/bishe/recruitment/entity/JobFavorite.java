package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job_favorite")
public class JobFavorite extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private Long jobseekerUserId;
}
