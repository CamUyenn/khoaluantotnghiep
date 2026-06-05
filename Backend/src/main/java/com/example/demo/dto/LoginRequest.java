package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập là bắt buộc")
    private String username;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;
}
