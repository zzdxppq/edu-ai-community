package com.edu.ai.core.auth.controller;

import com.edu.ai.common.exception.BizException;
import com.edu.ai.common.response.R;
import com.edu.ai.core.auth.dto.RegisterRequest;
import com.edu.ai.core.auth.dto.SendSmsRequest;
import com.edu.ai.core.auth.dto.UserDto;
import com.edu.ai.core.auth.service.AuthService;
import com.edu.ai.core.auth.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry points for auth (Story 1.2 AC1).
 * All errors flow through {@code GlobalExceptionHandler} — this controller
 * never writes R.fail directly.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SmsService smsService;
    private final AuthService authService;

    @PostMapping("/send-sms")
    public R<Void> sendSms(@Valid @RequestBody SendSmsRequest request) {
        try {
            smsService.sendVerificationCode(request.getPhone());
        } catch (BizException biz) {
            throw biz;
        } catch (RuntimeException ex) {
            // Redis / provider failures surface here; map to a stable 503 contract.
            throw new BizException(503, "短信发送失败，请稍后重试");
        }
        return R.success(null);
    }

    @PostMapping("/register")
    public R<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return R.success(authService.register(request));
    }
}
