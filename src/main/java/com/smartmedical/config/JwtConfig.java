package com.smartmedical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private long accessExpiration;
    private long refreshExpiration;
}
