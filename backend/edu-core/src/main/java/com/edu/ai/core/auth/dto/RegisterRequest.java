package com.edu.ai.core.auth.dto;

import com.edu.ai.common.constant.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "请输入正确的手机号")
    @Pattern(regexp = "^1[0-9]{10}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "验证码错误或已过期")
    @Pattern(regexp = "^\\d{6}$", message = "验证码错误或已过期")
    private String smsCode;

    @NotBlank(message = "用户名需2-20个字符")
    @Size(min = 2, max = 20, message = "用户名需2-20个字符")
    private String username;

    @NotNull(message = "请选择您的角色身份")
    private UserRole role;
}
