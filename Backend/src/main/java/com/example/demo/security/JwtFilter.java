package com.example.demo.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor

public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path != null && path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("Không tìm thấy Authorization header hợp lệ cho đường dẫn: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);

            if (username == null || role == null) {
                logger.warn("Token thiếu username hoặc role cho đường dẫn: {}", path);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null,
                    List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Xác thực thành công người dùng: {} với vai trò: {} cho đường dẫn: {}",
                    username, role, path);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("JWT đã hết hạn cho đường dẫn: {}", path);
            SecurityContextHolder.clearContext();
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            logger.warn("Định dạng JWT không hợp lệ cho đường dẫn: {}", path);
            SecurityContextHolder.clearContext();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.warn("Chữ ký JWT không hợp lệ cho đường dẫn: {}", path);
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            logger.error("Lỗi xử lý JWT cho đường dẫn: {}: {}", path, ex.getMessage(), ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
