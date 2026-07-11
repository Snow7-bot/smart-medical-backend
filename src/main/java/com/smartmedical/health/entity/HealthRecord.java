package com.smartmedical.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_record")
public class HealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long memberId;
    private String type;
    private String value;
    private String unit;
    private LocalDateTime recordedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
