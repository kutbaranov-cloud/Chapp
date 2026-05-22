package ru.denis.aestymes.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private Long id;
    private Long messageId; // Для операций правки/удаления
    private String content;
    private String senderNickname;
    private Long senderId;
    private LocalDateTime createdAt;
    private Long chatId;
    private Boolean isEdited;
    private String type; // NEW, EDIT, DELETE, READ_ALL, SEARCH_RESULT, GLOBAL_SEARCH_RESULT
    private Boolean isRead;

    // --- ВНЕДРЕНО ДЛЯ СИНХРОНИЗАЦИИ СПИСКА ЧАТОВ ---
    private Boolean wasUnread;
    private String newLastContent;
    private String newLastTime;

    // --- [НОВОЕ] ВНЕДРЕНО ДЛЯ ГЛОБАЛЬНОГО ПОИСКА ---
    private String chatName;

    // --- ХИРУРГИЧЕСКОЕ ДОПОЛНЕНИЕ: АВАТАРКА ОТПРАВИТЕЛЯ ---
    private String senderAvatar;

    // --- [НОВОЕ] ХИРУРГИЧЕСКОЕ ДОПОЛНЕНИЕ ДЛЯ REPLY / FORWARD ---
    private Long parentId;          // ID сообщения на которое отвечаем
    private String parentContent;   // Текст того сообщения (для цитаты)
    private String parentSenderName;// Имя отправителя того сообщения

    // --- [ДОБАВЛЕНО] ХИРУРГИЧЕСКОЕ ДОПОЛНЕНИЕ ДЛЯ АВАТАРКИ В ЦИТАТЕ/ПЕРЕСЫЛКЕ ---
    private String parentSenderAvatar; // Аватарка отправителя оригинала
}