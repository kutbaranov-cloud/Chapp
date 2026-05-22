package ru.denis.aestymes.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageContentRequest {
    private String content;
    private Long chatId;
}
