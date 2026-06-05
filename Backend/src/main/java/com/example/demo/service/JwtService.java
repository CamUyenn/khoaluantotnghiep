package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${auth.jwt.access-token.secret:mysecretkey-mysecretkey-mysecretkey-123-access}")
    private String accessSecret;

    @Value("${auth.jwt.refresh-token.secret:mysecretkey-mysecretkey-mysecretkey-123-refresh}")
    private String refreshSecret;

    @Value("${auth.jwt.access-token.ttl-seconds:900}")
    private long configuredAccessTokenTtlSeconds;

    @Value("${auth.jwt.refresh-token.ttl-days:7}")
    private long configuredRefreshTokenTtlDays;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    @PostConstruct
    void initKeys() {
        this.accessKey = buildKey(accessSecret, "khóa access token");
        this.refreshKey = buildKey(refreshSecret, "khóa refresh token");
    }

    private SecretKey buildKey(String secret, String secretLabel) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(secretLabel + " phải có ít nhất 32 byte");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }

    // Chức năng: tương thích ngược, trả access token.
    public String generateToken(String username, String role) {
        return generateAccessToken(username, role);
    }

    // Chức năng: tạo access token JWT.
    public String generateAccessToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenTtlMillis()))
                .signWith(accessKey)
                .compact();
    }

    // Chức năng: tạo refresh token JWT.
    public String generateRefreshToken(String username, String tokenId) {
        return Jwts.builder()
                .setSubject(username)
                .setId(tokenId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenTtlMillis()))
                .signWith(refreshKey)
                .compact();
    }

    // Chức năng: parse access token.
    private Claims extractAccessClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Chức năng: parse refresh token.
    private Claims extractRefreshClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Chức năng: trích xuất username từ access token.
    public String extractUsername(String token) {
        return extractAccessClaims(token).getSubject();
    }

    // Chức năng: trích xuất role từ access token.
    public String extractRole(String token) {
        return (String) extractAccessClaims(token).get("role");
    }

    // Chức năng: trích xuất username từ refresh token.
    public String extractUsernameFromRefreshToken(String refreshToken) {
        return extractRefreshClaims(refreshToken).getSubject();
    }

    // Chức năng: trích xuất tokenId (jti) từ refresh token.
    public String extractTokenIdFromRefreshToken(String refreshToken) {
        return extractRefreshClaims(refreshToken).getId();
    }

    // Chức năng: kiểm tra token có phải refresh token hay không.
    public boolean isRefreshToken(String refreshToken) {
        Object tokenType = extractRefreshClaims(refreshToken).get(TOKEN_TYPE_CLAIM);
        return REFRESH_TOKEN_TYPE.equals(tokenType);
    }

    // Chức năng: lấy TTL refresh token.
    public Duration getRefreshTokenTtlDuration() {
        return Duration.ofMillis(refreshTokenTtlMillis());
    }

    private long accessTokenTtlMillis() {
        return Math.max(60L, configuredAccessTokenTtlSeconds) * 1000L;
    }

    private long refreshTokenTtlMillis() {
        return Math.max(1L, configuredRefreshTokenTtlDays) * 24L * 60L * 60L * 1000L;
    }
}
