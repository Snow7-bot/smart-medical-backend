package com.smartmedical.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HealthRecordRequest {

    @NotNull
    private Long memberId;

    @NotBlank
    private String type;

    @NotBlank
    private String value;

    private String unit;
    private String recordedAt;
}
