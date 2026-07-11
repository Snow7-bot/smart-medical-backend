package com.smartmedical.consultation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_group")
public class ConversationGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
