package com.smartmedical.security;

import com.smartmedical.common.ErrorCode;
import com.smartmedical.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    /** 生成 access token */
    public String createAccessToken(Long userId, String phone) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessExpiration()))
                .signWith(getKey())
                .compact();
    }

    /** 生成 refresh token */
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration()))
                .signWith(getKey())
                .compact();
    }

    /** 从 token 中提取 userId */
    public Long getUserId(String token) {
        return Long.parseLong(parse(token).getPayload().getSubject());
    }

    /** 校验 token 是否有效 */
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token);
    }
}
