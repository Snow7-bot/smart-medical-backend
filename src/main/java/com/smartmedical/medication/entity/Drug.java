package com.smartmedical.medication.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("drug")
public class Drug {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long memberId;
    private String name;
    private String spec;
    private String dose;
    private String freq;
    private String timeOfDay;
    private Integer warning;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
