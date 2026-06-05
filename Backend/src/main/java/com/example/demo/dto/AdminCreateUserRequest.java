package com.example.demo.dto;

import com.example.demo.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Tên đăng nhập là bắt buộc")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{4,50}$", message = "Tên đăng nhập phải từ 4-50 ký tự và không chứa khoảng trắng")
    private String username;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
    private String password;

    @NotNull(message = "Vai trò là bắt buộc")
    private Role role;

    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
    private String phoneNumber;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    private Boolean isActive;
}
