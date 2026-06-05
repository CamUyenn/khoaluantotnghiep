package com.example.demo.dto;

import com.example.demo.entity.Role;

import lombok.Data;

@Data
public class AdminUserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String phoneNumber;
    private String email;
    private Role role;
    private Boolean isActive;

    public AdminUserResponse() {
    }

    public AdminUserResponse(Long id, String username, String fullName, String phoneNumber, String email, Role role,
            Boolean isActive) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
    }
}
