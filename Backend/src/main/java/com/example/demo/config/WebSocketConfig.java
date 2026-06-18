package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Tạo một kênh phát sóng có tên bắt đầu bằng "/topic"
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Frontend sẽ gọi vào đường dẫn "/ws-clinic" để nối ống nước
        registry.addEndpoint("/ws-clinic")
                .setAllowedOriginPatterns("*") // Cho phép Next.js truy cập
                .withSockJS(); // Fallback nếu trình duyệt cũ không hỗ trợ WebSockets
    }
}
