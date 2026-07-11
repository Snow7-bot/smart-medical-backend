package com.smartmedical.medical.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long patientId;
    private String fileUrl;
    private String fileType;
    private String parsedData;
    private String parseStatus;
    private LocalDate recordDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
