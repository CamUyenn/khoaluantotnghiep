package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionistRoomSuggestionResponse {
    private Long roomId;
    private String roomName;
    private String specialty;
}