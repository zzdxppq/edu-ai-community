package com.edu.ai.core.auth.service;

import com.edu.ai.common.constant.UserType;
import com.edu.ai.common.exception.BizException;
import com.edu.ai.core.auth.dto.RegisterRequest;
import com.edu.ai.core.auth.dto.UserDto;
import com.edu.ai.core.auth.entity.User;
import com.edu.ai.core.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration orchestrator (Story 1.2 AC1). Order is deliberate:
 * verifyCode → existsByPhone → save. {@code verifyCode} is first so a wrong
 * code short-circuits before we touch the users table; on success it also
 * DELs the Redis key (one-time consumption).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SmsService smsService;
    private final UserRepository userRepository;

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (!smsService.verifyCode(request.getPhone(), request.getSmsCode())) {
            throw new BizException(400, "验证码错误或已过期");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BizException(409, "该手机号已注册，请直接登录");
        }
        User user = new User();
        user.setPhone(request.getPhone());
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setUserType(UserType.USER);
        User saved = userRepository.save(user);
        return new UserDto(
                saved.getId(),
                saved.getPhone(),
                saved.getUsername(),
                saved.getRole(),
                saved.getUserType(),
                saved.getCreatedAt()
        );
    }
}
