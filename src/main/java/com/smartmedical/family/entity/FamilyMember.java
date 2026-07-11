package com.smartmedical.family.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("family_member")
public class FamilyMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String relation;
    private Integer gender;
    private LocalDate birthDate;
    private String avatarUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
