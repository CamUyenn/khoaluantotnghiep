package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.ResetPasswordWithOtpRequest;
import com.example.demo.dto.VerifyForgotPasswordOtpRequest;
import com.example.demo.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Đăng nhập và đăng ký tài khoản bệnh nhân. Nhóm này không yêu cầu JWT.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Đăng nhập bằng username/password và nhận access token JWT.")
    // Chức năng: xử lý đăng nhập.
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword());
    }

    // Chức năng: xử lý bệnh nhân đăng ký.
    @PostMapping("/register/patient")
    @Operation(summary = "Đăng ký bệnh nhân", description = "Tạo tài khoản bệnh nhân mới và trả về bộ token đăng nhập.")
    public AuthResponse registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
        return authService.registerPatient(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token", description = "Nhận refresh token hợp lệ, quay vòng refresh token và trả về cặp token mới.")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Thu hồi refresh token hiện tại trong Redis.")
    public String logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return "Đăng xuất thành công";
    }

    @PostMapping("/forgot-password/send-otp")
    @Operation(summary = "Gửi OTP quên mật khẩu", description = "Nhận email, tạo OTP và gửi OTP qua email cho người dùng.")
    public String sendForgotPasswordOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendForgotPasswordOtp(request);
        return "Gửi OTP thành công";
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(summary = "Xác thực OTP quên mật khẩu", description = "Xác thực mã OTP từ email để cho phép đặt lại mật khẩu.")
    public String verifyForgotPasswordOtp(@Valid @RequestBody VerifyForgotPasswordOtpRequest request) {
        authService.verifyForgotPasswordOtp(request);
        return "Xác thực OTP thành công";
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Đặt lại mật khẩu sau OTP", description = "Cập nhật mật khẩu mới sau khi OTP đã được xác thực hợp lệ.")
    public String resetPasswordWithOtp(@Valid @RequestBody ResetPasswordWithOtpRequest request) {
        authService.resetPasswordWithOtp(request);
        return "Cập nhật mật khẩu thành công";
    }
}
