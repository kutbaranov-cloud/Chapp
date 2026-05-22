package ru.denis.aestymes.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "name")
    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Прямой статус: "online" или "offline".
     * Это основной источник правды для системы.
     */
    @Column(name = "status")
    private String status = "offline";

    /**
     * Время последней активности (для отображения "был в сети...")
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_verified")
    private boolean isVerified;

    @Column(name = "confirmation_token")
    private String confirmationToken;

    /**
     * Метка времени последнего логаута.
     * Используется в MyUserService для блокировки "случайных" входов в течение нескольких секунд.
     */
    @Column(name = "last_logout")
    private LocalDateTime lastLogout;

    /**
     * Метод для быстрой проверки состояния.
     * Мы считаем пользователя онлайн ТОЛЬКО если поле status равно "online".
     */
    @Transient
    public boolean isOnline() {
        return "online".equalsIgnoreCase(this.status);
    }
}