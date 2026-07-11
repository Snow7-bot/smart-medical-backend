package com.smartmedical.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.security.JwtTokenProvider;
import com.smartmedical.user.dto.*;
import com.smartmedical.user.entity.User;
import com.smartmedical.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String CODE_PREFIX = "sms:code:";
    private static final String CODE_RATE_PREFIX = "sms:rate:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_TTL = Duration.ofSeconds(60);

    @Override
    public void sendCode(SendCodeRequest request) {
        String phone = request.getPhone();

        String rateKey = CODE_RATE_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            throw new BusinessException(ErrorCode.CODE_TOO_FREQUENT);
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set(CODE_PREFIX + phone, code, CODE_TTL);
        redisTemplate.opsForValue().set(rateKey, "1", RATE_TTL);

        log.info("验证码已发送: phone={}, code={}", phone, code);
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();
        String password = request.getPassword();

        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getPhone, phone))) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        if (code != null && !code.isEmpty()) {
            String savedCode = redisTemplate.opsForValue().get(CODE_PREFIX + phone);
            if (savedCode == null) {
                throw new BusinessException(ErrorCode.CODE_EXPIRED);
            }
            if (!savedCode.equals(code)) {
                throw new BusinessException(ErrorCode.CODE_WRONG);
            }
            redisTemplate.delete(CODE_PREFIX + phone);
        }

        validatePassword(password);

        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(request.getName() != null ? request.getName() : "用户" + phone.substring(phone.length() - 4));
        user.setStatus(1);
        userMapper.insert(user);

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));

        if (user == null) {
            throw new BusinessException(ErrorCode.PHONE_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WRONG);
        }
        if (user.getStatus() == 2) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return new LoginResponse(null, user.getName(), user.getPhone());
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = new User();
        user.setId(userId);
        user.setName(request.getName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());
        user.setBirthDate(request.getBirthDate());
        userMapper.updateById(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getPhone());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                "refresh:" + user.getId(),
                refreshToken,
                Duration.ofMillis(604800000));

        return new LoginResponse(token, user.getName(), user.getPhone());
    }

    private void validatePassword(String password) {
        if (password.length() < 6 || password.length() > 20) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_WEAK);
        }
    }
}
