package com.bishe.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("skill_dict")
public class SkillDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skillName;

    private String category;
}
