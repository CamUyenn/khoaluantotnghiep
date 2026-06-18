package com.example.demo.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ChatbotAskRequest;
import com.example.demo.dto.ChatbotAskResponse;
import com.example.demo.dto.ChatbotHistoryItemResponse;
import com.example.demo.service.GeminiChatbotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "AI Chatbot", description = "Chatbot hỏi đáp y tế tham khảo sử dụng Gemini API")
public class ChatbotController {

    private final GeminiChatbotService geminiChatbotService;

    @PostMapping("/ask")
    @Operation(summary = "Hỏi AI Chatbot", description = "Nhận câu hỏi người dùng và trả về phản hồi từ Gemini")
    // Chức năng: xử lý yêu cầu hỏi đáp chatbot AI.
    public ChatbotAskResponse ask(
            Authentication authentication,
            @Valid @RequestBody ChatbotAskRequest request) {
        String username = authentication == null ? null : authentication.getName();
        return geminiChatbotService.ask(request.getMessage(), username);
    }

    @GetMapping("/history")
    @Operation(summary = "Lấy lịch sử hội thoại", description = "Trả về lịch sử hội thoại của user/patient đang đăng nhập để dùng lại khi tái khám")
    // Chức năng: xử lý lấy lịch sử chatbot theo user/patient.
    public List<ChatbotHistoryItemResponse> getHistory(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        return geminiChatbotService.getHistory(username);
    }

    @DeleteMapping("/history")
    @Operation(summary = "Xóa lịch sử hội thoại", description = "Xóa lịch sử chatbot của user/patient đang đăng nhập")
    // Chức năng: xử lý xóa lịch sử chatbot theo user/patient.
    public String clearHistory(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        long deletedCount = geminiChatbotService.clearHistory(username);
        return "Đã xóa " + deletedCount + " tin nhắn lịch sử chatbot";
    }
}
