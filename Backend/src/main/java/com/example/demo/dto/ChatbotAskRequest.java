package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatbotAskRequest {

    @NotBlank(message = "Nội dung câu hỏi là bắt buộc")
    @Size(max = 2000, message = "Nội dung câu hỏi không được vượt quá 2000 ký tự")
    private String message;
}
