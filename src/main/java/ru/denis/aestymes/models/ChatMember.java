package ru.denis.aestymes.models;

import jakarta.persistence.*;
import lombok.*;
import ru.denis.aestymes.dtos.ChatMemberRole;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_id", "user_id"})
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name  = "user_id", nullable = false)
    private MyUser user;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    @Column
    private ChatMemberRole role = ChatMemberRole.member;

    @Column(name = "is_banned")
    private Boolean isBanned = false;

    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    // ВНЕДРЕНО ХИРУРГИЧЕСКИ: Счетчик непрочитанных сообщений
    @Column(name = "unread_count")
    private Integer unreadCount = 0;

    @PrePersist
    protected void onCreated() {
        joinedAt = LocalDateTime.now();
        if (this.unreadCount == null) {
            this.unreadCount = 0;
        }
    }
}