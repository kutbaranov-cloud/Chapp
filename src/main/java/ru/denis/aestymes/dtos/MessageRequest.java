package ru.denis.aestymes.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageRequest {
    private Long chatId;
    private Long senderId;
    private String content;
}
