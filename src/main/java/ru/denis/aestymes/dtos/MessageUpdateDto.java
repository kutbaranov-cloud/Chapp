package ru.denis.aestymes.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageUpdateDto {
    private String type;      // "EDIT" или "DELETE"
    private Long messageId;   // Какое сообщение трогаем
    private String content;   // Новый текст (только для EDIT)
    private Long chatId;      // В какой чат отправить уведомление
}