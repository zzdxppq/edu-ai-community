package com.eduai.community.service;

import com.eduai.community.model.dto.AuthRequest;
import com.eduai.community.model.dto.RegisterRequest;
import com.eduai.community.model.entity.User;
import com.eduai.community.repository.UserRepository;
import com.eduai.community.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String MOCK_SMS_CODE = "123456";
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;

    public void sendSmsCode(String phone) {
        String key = SMS_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(key, MOCK_SMS_CODE, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("Mock SMS code sent to {}: {}", phone, MOCK_SMS_CODE);
    }

    public boolean verifySmsCode(String phone, String code) {
        String key = SMS_CODE_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public Map<String, Object> register(RegisterRequest request) {
        if (!verifySmsCode(request.getPhone(), request.getSmsCode())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        Optional<User> existing = userRepository.findByPhone(request.getPhone());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("该手机号已注册");
        }

        User user = User.builder()
                .phone(request.getPhone())
                .username(request.getUsername())
                .role(request.getRole())
                .build();
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    public Map<String, Object> login(AuthRequest request) {
        if (!verifySmsCode(request.getPhone(), request.getSmsCode())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("该手机号未注册，请先注册"));

        return buildAuthResponse(user);
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("刷新令牌无效或已过期");
        }

        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("令牌类型不正确");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        return buildAuthResponse(user);
    }

    private Map<String, Object> buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshTokenStr = jwtUtil.generateRefreshToken(user.getId());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("phone", user.getPhone());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());
        userInfo.put("region", user.getRegion());
        userInfo.put("school", user.getSchool());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshTokenStr);
        result.put("user", userInfo);

        return result;
    }
}
