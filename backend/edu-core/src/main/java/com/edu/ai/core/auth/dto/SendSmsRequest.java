package com.edu.ai.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendSmsRequest {

    @NotBlank(message = "请输入正确的手机号")
    @Pattern(regexp = "^1[0-9]{10}$", message = "请输入正确的手机号")
    private String phone;
}
