package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.example.demo.exception.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:token:";

    private final StringRedisTemplate redisTemplate;

    // Chức năng: lưu refresh token dạng hash trong Redis với TTL.
    public void storeRefreshToken(String username, String tokenId, String rawRefreshToken, Duration ttl) {
        String key = buildKey(username);
        String value = tokenId + ":" + hashToken(rawRefreshToken);

        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }
    }

    // Chức năng: xác thực refresh token khớp với phiên đang lưu trong Redis.
    public void validateRefreshTokenOrThrow(String username, String tokenId, String rawRefreshToken) {
        String key = buildKey(username);
        String expectedValue;

        try {
            expectedValue = redisTemplate.opsForValue().get(key);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }

        if (expectedValue == null) {
            throw AppException.of(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String providedValue = tokenId + ":" + hashToken(rawRefreshToken);
        if (!constantTimeEquals(expectedValue, providedValue)) {
            throw AppException.of(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }
    }

    // Chức năng: thu hồi toàn bộ refresh token đang hoạt động của user.
    public void revokeRefreshTokens(String username) {
        String key = buildKey(username);
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException ex) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE, "Redis hiện không khả dụng");
        }
    }

    private String buildKey(String username) {
        return REFRESH_TOKEN_KEY_PREFIX + username;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Thuật toán SHA-256 hiện không khả dụng", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }
}
