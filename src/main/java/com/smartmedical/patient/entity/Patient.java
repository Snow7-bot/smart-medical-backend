package com.smartmedical.patient.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("patient")
public class Patient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String medicalHistory;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
