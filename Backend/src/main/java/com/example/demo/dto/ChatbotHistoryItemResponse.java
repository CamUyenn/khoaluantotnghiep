package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatbotHistoryItemResponse {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
