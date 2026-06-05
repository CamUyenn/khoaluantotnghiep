package com.example.demo.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.ChatbotMessage;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {

    List<ChatbotMessage> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    long deleteByUser_Id(Long userId);
}
