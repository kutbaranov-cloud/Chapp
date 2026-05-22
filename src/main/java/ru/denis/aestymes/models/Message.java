package ru.denis.aestymes.models;

import jakarta.persistence.*;
import lombok.*;
import ru.denis.aestymes.dtos.MessageType;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
я
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private MyUser sender;

    @Column(nullable = false)
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType = MessageType.text;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyTo;

    // --- ХИРУРГИЧЕСКОЕ ДОПОЛНЕНИЕ ДЛЯ ОТОБРАЖЕНИЯ ЦИТАТ И REPLY (БЕЗ ОШИБОК) ---
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "parent_content")
    private String parentContent;

    @Column(name = "parent_sender_name")
    private String parentSenderName;

    // [ДОБАВЛЕНО] Поле для хранения URL аватарки автора пересланного сообщения
    @Column(name = "parent_sender_avatar")
    private String parentSenderAvatar;
    // -------------------------------------------------------------------------

    // --- ХИРУРГИЧЕСКОЕ ДОПОЛНЕНИЕ ДЛЯ ЗАКРЕПЛЕНИЯ СООБЩЕНИЙ ---
    @Builder.Default
    @Column(name = "is_pinned")
    private Boolean isPinned = false;
    // -------------------------------------------------------------------------

    @Column(name = "encryption_key")
    private String encryptionKey;

    public Boolean getIsRead() {
        return isRead != null && isRead;
    }

    // --- ХИРУРГИЧЕСКИЕ МЕТОДЫ ГЕТТЕРА И СЕТТЕРА ДЛЯ ИСКЛЮЧЕНИЯ ОШИБОК СБОРКИ ---
    public Boolean getIsPinned() {
        return isPinned != null && isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}