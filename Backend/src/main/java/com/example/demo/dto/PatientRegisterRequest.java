package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientRegisterRequest {

    @NotBlank(message = "Tên đăng nhập là bắt buộc")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{4,50}$", message = "Tên đăng nhập phải từ 4-50 ký tự và không chứa khoảng trắng")
    private String username;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @Size(max = 10, message = "Giới tính tối đa 10 ký tự")
    private String gender;

    @Size(max = 20, message = "Số CCCD/CMND tối đa 20 ký tự")
    private String nationalId;

    @Size(max = 20, message = "Số BHYT tối đa 20 ký tự")
    private String healthInsuranceNumber;

    @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
    private String phoneNumber;

    @JsonAlias("gmail")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;
}
