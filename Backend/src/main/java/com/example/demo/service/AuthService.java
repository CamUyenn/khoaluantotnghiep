package com.example.demo.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.exception.AppException;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.dto.ResetPasswordWithOtpRequest;
import com.example.demo.dto.VerifyForgotPasswordOtpRequest;
import com.example.demo.entity.Patient;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthService {

    private static final String RESET_OTP_KEY_PREFIX = "auth:forgot-password:otp:";
    private static final String RESET_OTP_VERIFIED_KEY_PREFIX = "auth:forgot-password:verified:";
    private static final String RESET_OTP_COOLDOWN_KEY_PREFIX = "auth:forgot-password:cooldown:";
    private static final String RESET_OTP_SEND_COUNT_KEY_PREFIX = "auth:forgot-password:send-count:";
    private static final String RESET_OTP_INVALID_COUNT_KEY_PREFIX = "auth:forgot-password:invalid-count:";

    @Value("${auth.forgot-password.otp.ttl-minutes:10}")
    private long resetOtpTtlMinutes;

    @Value("${auth.forgot-password.otp.cooldown-seconds:60}")
    private long resetOtpCooldownSeconds;

    @Value("${auth.forgot-password.otp.send-limit-window-minutes:60}")
    private long resetOtpSendLimitWindowMinutes;

    @Value("${auth.forgot-password.otp.max-send-attempts:5}")
    private int resetOtpMaxSendAttempts;

    @Value("${auth.forgot-password.otp.max-invalid-attempts:5}")
    private int resetOtpMaxInvalidAttempts;

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenService refreshTokenService;

    private final SecureRandom secureRandom = new SecureRandom();

    // Chức năng: xử lý login.
    public AuthResponse login(String username, String password) {
        String loginKey = trimToNull(username);
        User user = userRepository.findByUsername(loginKey)
                .or(() -> userRepository.findByEmailIgnoreCase(loginKey))
                .orElseThrow(
                        () -> AppException.of(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Tài khoản đang bị vô hiệu hóa");
        }

        String stored = user.getPasswordHash();
        boolean valid = false;
        if (stored != null) {
            try {
                valid = passwordEncoder.matches(password, stored);
            } catch (IllegalArgumentException ignored) {
                // Các giá trị văn bản thuần túy cũ được xử lý bằng phương án dự phòng bên dưới.
            }
        }

        // Đảm bảo khả năng tương thích ngược với các giá trị văn bản thuần túy cũ trong
        // cột mật khẩu.
        if (!valid && stored != null && stored.equals(password)) {
            user.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(user);
            valid = true;
        }

        if (!valid) {
            throw AppException.of(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu");
        }

        return issueAuthTokens(user);
    }

    // Chức năng: xử lý register patient.
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        String normalizedUsername = request.getUsername().trim();
        String normalizedPhoneNumber = trimToNull(request.getPhoneNumber());
        String normalizedEmail = normalizeEmailOptional(request.getEmail());

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw AppException.of(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }

        if (request.getNationalId() != null
                && !request.getNationalId().isBlank()
                && patientRepository.existsByNationalId(request.getNationalId().trim())) {
            throw AppException.of(HttpStatus.CONFLICT, "Số CCCD/CMND đã tồn tại");
        }

        if (normalizedPhoneNumber != null
                && (userRepository.existsByPhoneNumber(normalizedPhoneNumber)
                        || patientRepository.existsByPhoneNumber(normalizedPhoneNumber))) {
            throw AppException.of(HttpStatus.CONFLICT, "Số điện thoại đã tồn tại");
        }

        if (normalizedEmail != null
                && (userRepository.existsByEmailIgnoreCase(normalizedEmail)
                        || patientRepository.existsByGmail(normalizedEmail))) {
            throw AppException.of(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PATIENT);
        user.setIsActive(true);
        user = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setFullName(request.getFullName().trim());
        patient.setGender(trimToNull(request.getGender()));
        patient.setNationalId(trimToNull(request.getNationalId()));
        patient.setHealthInsuranceNumber(trimToNull(request.getHealthInsuranceNumber()));
        patient.setPhoneNumber(normalizedPhoneNumber);
        patient.setGmail(normalizedEmail);
        patientRepository.save(patient);

        return issueAuthTokens(user);
    }

    // Chức năng: quay vòng refresh token và cấp access token mới.
    public AuthResponse refreshToken(String refreshToken) {
        String normalizedRefreshToken = trimToNull(refreshToken);
        if (normalizedRefreshToken == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Bắt buộc cung cấp refresh token");
        }

        String username;
        String tokenId;
        try {
            if (!jwtService.isRefreshToken(normalizedRefreshToken)) {
                throw AppException.of(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ");
            }

            username = jwtService.extractUsernameFromRefreshToken(normalizedRefreshToken);
            tokenId = jwtService.extractTokenIdFromRefreshToken(normalizedRefreshToken);
        } catch (ExpiredJwtException ex) {
            throw AppException.of(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn");
        } catch (JwtException | IllegalArgumentException ex) {
            throw AppException.of(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ");
        }

        if (tokenId == null || tokenId.isBlank()) {
            throw AppException.of(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.of(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Tài khoản đang bị vô hiệu hóa");
        }

        refreshTokenService.validateRefreshTokenOrThrow(username, tokenId, normalizedRefreshToken);
        return issueAuthTokens(user);
    }

    // Chức năng: thu hồi refresh token khi logout.
    public void logout(String refreshToken) {
        String normalizedRefreshToken = trimToNull(refreshToken);
        if (normalizedRefreshToken == null) {
            return;
        }

        try {
            if (!jwtService.isRefreshToken(normalizedRefreshToken)) {
                return;
            }

            String username = jwtService.extractUsernameFromRefreshToken(normalizedRefreshToken);
            refreshTokenService.revokeRefreshTokens(username);
        } catch (JwtException | IllegalArgumentException ignored) {
            // Logout API is idempotent: invalid/expired token should still return success.
        }
    }

    // Chức năng: gửi OTP cho luồng quên mật khẩu.
    public void sendForgotPasswordOtp(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = findUserByEmailForReset(normalizedEmail);

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Tài khoản đang bị vô hiệu hóa");
        }

        enforceOtpSendLimitOrThrow(user.getId());

        String otp = generateOtp();
        saveOtpToRedis(user.getId(), otp);
        try {
            notificationService.sendForgotPasswordOtp(normalizedEmail, otp);
            recordSuccessfulOtpSend(user.getId());
        } catch (RuntimeException ex) {
            clearResetOtpSilently(user.getId());
            throw ex;
        }
    }

    // Chức năng: xác thực OTP đã nhập từ email.
    public void verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = findUserByEmailForReset(normalizedEmail);

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Tài khoản đang bị vô hiệu hóa");
        }

        validateOtpOrThrow(user.getId(), request.getOtp().trim());
        markOtpVerified(user.getId());
    }

    // Chức năng: reset mật khẩu sau khi OTP đã được xác thực.
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = findUserByEmailForReset(normalizedEmail);

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw AppException.of(HttpStatus.FORBIDDEN, "Tài khoản đang bị vô hiệu hóa");
        }

        ensureOtpVerifiedOrThrow(user.getId());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        clearResetOtp(user.getId());
        refreshTokenService.revokeRefreshTokens(user.getUsername());
        userRepository.save(user);
    }

    // Chức năng: phát hành cặp access/refresh token mới và lưu refresh token vào
    // Redis.
    private AuthResponse issueAuthTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user.getUsername(), refreshTokenId);

        refreshTokenService.storeRefreshToken(
                user.getUsername(),
                refreshTokenId,
                refreshToken,
                jwtService.getRefreshTokenTtlDuration());

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    // Chức năng: xử lý tạo phản hồi xác thực.
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        return response;
    }

    // Chức năng: xử lý trim to null.
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chức năng: chuẩn hóa email tùy chọn để lưu và kiểm tra trùng.
    private String normalizeEmailOptional(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    // Chức năng: chuẩn hóa email để tìm người dùng quên mật khẩu.
    private String normalizeEmail(String email) {
        String normalized = normalizeEmailOptional(email);
        if (normalized == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Email là bắt buộc");
        }
        return normalized;
    }

    // Chức năng: tìm user theo email người dùng, email bệnh nhân hoặc username dạng
    // email.
    private User findUserByEmailForReset(String normalizedEmail) {
        User userByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);
        if (userByEmail != null) {
            return userByEmail;
        }

        Patient patient = patientRepository.findByGmailIgnoreCase(normalizedEmail)
                .orElse(null);
        if (patient != null && patient.getUser() != null) {
            return patient.getUser();
        }

        return userRepository.findByUsernameIgnoreCase(normalizedEmail)
                .filter(candidate -> isValidEmail(candidate.getUsername()))
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy email"));
    }

    // Chức năng: tạo OTP 6 chữ số ngẫu nhiên.
    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    // Chức năng: lưu OTP vào Redis với TTL cấu hình.
    private void saveOtpToRedis(Long userId, String otp) {
        String otpKey = buildOtpKey(userId);
        String verifiedKey = buildVerifiedKey(userId);
        String invalidCountKey = buildInvalidCountKey(userId);
        try {
            redisTemplate.opsForValue().set(otpKey, otp, otpTtlDuration());
            redisTemplate.delete(verifiedKey);
            redisTemplate.delete(invalidCountKey);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }
    }

    // Chức năng: kiểm tra cooldown và giới hạn số lần gửi OTP.
    private void enforceOtpSendLimitOrThrow(Long userId) {
        String cooldownKey = buildCooldownKey(userId);
        String sendCountKey = buildSendCountKey(userId);
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
                throw AppException.of(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Vui lòng chờ " + otpCooldownSeconds() + " giây trước khi yêu cầu OTP lại");
            }

            String rawSendCount = redisTemplate.opsForValue().get(sendCountKey);
            long sendCount = parseLongSafely(rawSendCount);
            if (sendCount >= otpMaxSendAttempts()) {
                throw AppException.of(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Bạn đã yêu cầu OTP quá nhiều lần. Vui lòng thử lại sau");
            }
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }
    }

    // Chức năng: ghi nhận gửi OTP thành công để áp cooldown và hạn mức gửi.
    private void recordSuccessfulOtpSend(Long userId) {
        String cooldownKey = buildCooldownKey(userId);
        String sendCountKey = buildSendCountKey(userId);
        try {
            redisTemplate.opsForValue().set(cooldownKey, "1", otpCooldownDuration());

            Long sendCount = redisTemplate.opsForValue().increment(sendCountKey);
            if (sendCount != null && sendCount == 1L) {
                redisTemplate.expire(sendCountKey, otpSendLimitWindowDuration());
            }
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }
    }

    // Chức năng: xác thực OTP và trạng thái hết hạn.
    private void validateOtpOrThrow(Long userId, String otp) {
        String otpKey = buildOtpKey(userId);
        String expectedOtp;

        try {
            expectedOtp = redisTemplate.opsForValue().get(otpKey);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }

        if (expectedOtp == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Bạn chưa yêu cầu OTP");
        }

        if (!expectedOtp.equals(otp)) {
            registerInvalidOtpAttemptOrThrow(userId, otpKey);
        }
    }

    // Chức năng: theo dõi số lần nhập OTP sai và khóa khi vượt ngưỡng.
    private void registerInvalidOtpAttemptOrThrow(Long userId, String otpKey) {
        String invalidCountKey = buildInvalidCountKey(userId);
        try {
            Long invalidCount = redisTemplate.opsForValue().increment(invalidCountKey);
            if (invalidCount != null && invalidCount == 1L) {
                Long remainingSeconds = redisTemplate.getExpire(otpKey);
                if (remainingSeconds != null && remainingSeconds > 0) {
                    redisTemplate.expire(invalidCountKey, Duration.ofSeconds(remainingSeconds));
                } else {
                    redisTemplate.expire(invalidCountKey, otpTtlDuration());
                }
            }

            if (invalidCount != null && invalidCount >= otpMaxInvalidAttempts()) {
                clearResetOtp(userId);
                throw AppException.of(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu OTP mới");
            }
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }

        throw AppException.of(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ");
    }

    // Chức năng: đánh dấu OTP đã verify trong Redis với cùng TTL còn lại.
    private void markOtpVerified(Long userId) {
        String otpKey = buildOtpKey(userId);
        String verifiedKey = buildVerifiedKey(userId);
        String invalidCountKey = buildInvalidCountKey(userId);

        Long remainingSeconds;
        try {
            remainingSeconds = redisTemplate.getExpire(otpKey);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }

        if (remainingSeconds == null || remainingSeconds <= 0) {
            clearResetOtp(userId);
            throw AppException.of(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn");
        }

        try {
            redisTemplate.opsForValue().set(verifiedKey, "1", Duration.ofSeconds(remainingSeconds));
            redisTemplate.delete(invalidCountKey);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }
    }

    // Chức năng: chỉ cho phép reset khi OTP đã xác thực và còn hiệu lực.
    private void ensureOtpVerifiedOrThrow(Long userId) {
        String verifiedKey = buildVerifiedKey(userId);
        Boolean exists;
        try {
            exists = redisTemplate.hasKey(verifiedKey);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }

        if (!Boolean.TRUE.equals(exists)) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "OTP chưa được xác thực");
        }
    }

    // Chức năng: xóa trạng thái OTP trên Redis sau khi dùng hoặc hết hạn.
    private void clearResetOtp(Long userId) {
        try {
            redisTemplate.delete(buildOtpKey(userId));
            redisTemplate.delete(buildVerifiedKey(userId));
            redisTemplate.delete(buildInvalidCountKey(userId));
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }
    }

    private void clearResetOtpSilently(Long userId) {
        try {
            clearResetOtp(userId);
        } catch (AppException ignored) {
            // Keep original exception from email/flow operation.
        }
    }

    private String buildOtpKey(Long userId) {
        return RESET_OTP_KEY_PREFIX + userId;
    }

    private String buildVerifiedKey(Long userId) {
        return RESET_OTP_VERIFIED_KEY_PREFIX + userId;
    }

    private String buildCooldownKey(Long userId) {
        return RESET_OTP_COOLDOWN_KEY_PREFIX + userId;
    }

    private String buildSendCountKey(Long userId) {
        return RESET_OTP_SEND_COUNT_KEY_PREFIX + userId;
    }

    private String buildInvalidCountKey(Long userId) {
        return RESET_OTP_INVALID_COUNT_KEY_PREFIX + userId;
    }

    private long parseLongSafely(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private Duration otpTtlDuration() {
        return Duration.ofMinutes(Math.max(1L, resetOtpTtlMinutes));
    }

    private Duration otpCooldownDuration() {
        return Duration.ofSeconds(otpCooldownSeconds());
    }

    private long otpCooldownSeconds() {
        return Math.max(1L, resetOtpCooldownSeconds);
    }

    private Duration otpSendLimitWindowDuration() {
        return Duration.ofMinutes(Math.max(1L, resetOtpSendLimitWindowMinutes));
    }

    private int otpMaxSendAttempts() {
        return Math.max(1, resetOtpMaxSendAttempts);
    }

    private int otpMaxInvalidAttempts() {
        return Math.max(1, resetOtpMaxInvalidAttempts);
    }

    private boolean isValidEmail(String value) {
        if (value == null) {
            return false;
        }
        String email = value.trim();
        int atIndex = email.indexOf('@');
        int lastDotIndex = email.lastIndexOf('.');
        return atIndex > 0 && lastDotIndex > atIndex + 1 && lastDotIndex < email.length() - 1;
    }

}
