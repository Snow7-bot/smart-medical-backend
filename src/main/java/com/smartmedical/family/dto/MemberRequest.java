package com.smartmedical.family.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "关系不能为空")
    private String relation;

    private String gender;
    private LocalDate birthDate;
    private String avatarUrl;
}
