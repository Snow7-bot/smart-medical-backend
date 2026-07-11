package com.smartmedical.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String name;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthDate;
}
