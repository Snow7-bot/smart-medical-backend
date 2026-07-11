package com.smartmedical.consultation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attachment")
public class Attachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
