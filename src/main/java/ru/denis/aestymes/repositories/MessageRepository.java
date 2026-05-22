package ru.denis.aestymes.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.denis.aestymes.models.Chat;
import ru.denis.aestymes.models.Message;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatOrderByCreatedAtAsc(Chat chat);

    Optional<Message> findFirstByChatOrderByCreatedAtDesc(Chat chat);

    List<Message> findByChatIdAndContentContainingIgnoreCase(Long chatId, String content);

    // --- ХИРУРГИЧЕСКОЕ ДОПОЛНЕНИЕ ДЛЯ УДАЛЕНИЯ: поиск всех ответов на конкретное сообщение ---
    List<Message> findAllByReplyTo(Message replyTo);

    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.chat c " +
            "JOIN FETCH c.members mb " +
            "JOIN FETCH mb.user " +
            "WHERE LOWER(m.content) LIKE LOWER(concat('%', :content, '%')) " +
            "AND c.id IN :chatIds")
    List<Message> findByContentContainingIgnoreCaseAndChatIdIn(@Param("content") String content, @Param("chatIds") List<Long> chatIds);

    @Transactional
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.chat = :chat AND m.sender.id <> :userId AND m.isRead = false")
    void markMessagesAsRead(@Param("chat") Chat chat, @Param("userId") Long userId);
}