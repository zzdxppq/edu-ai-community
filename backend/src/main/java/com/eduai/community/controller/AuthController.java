package com.eduai.community.controller;

import com.eduai.community.model.dto.AuthRequest;
import com.eduai.community.model.dto.RegisterRequest;
import com.eduai.community.service.AuthService;
import com.eduai.community.util.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-sms")
    public ApiResponse<Void> sendSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return ApiResponse.error(400, "手机号不能为空");
        }
        authService.sendSmsCode(phone);
        return ApiResponse.success("验证码发送成功", null);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Map<String, Object> result = authService.register(request);
            return ApiResponse.success("注册成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AuthRequest request) {
        try {
            Map<String, Object> result = authService.login(request);
            return ApiResponse.success("登录成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.error(400, "刷新令牌不能为空");
        }
        try {
            Map<String, Object> result = authService.refreshToken(refreshToken);
            return ApiResponse.success("刷新成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }
}
