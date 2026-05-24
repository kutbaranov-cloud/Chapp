package ru.denis.aestymes.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.denis.aestymes.models.Chat;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    // ХИРУРГИЧЕСКОЕ ДОБАВЛЕНИЕ: Жадная загрузка для WebSocket
    @Query("SELECT c FROM Chat c LEFT JOIN FETCH c.members WHERE c.id = :id")
    Optional<Chat> findByIdWithMembers(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Chat c JOIN c.members m WHERE m.user.id = :userId AND c.id IS NOT NULL AND m.isBanned = false ORDER BY c.updatedAt DESC")
    List<Chat> findChatsByUserId(@Param("userId") Long userId);

    // ХИРУРГИЧЕСКАЯ МОДИФИКАЦИЯ: Теперь запрос находит чат независимо от того, кто был передан первым (id1 или id2)
    @Query("SELECT c FROM Chat c JOIN c.members m1 JOIN c.members m2 " +
            "WHERE c.isGroupChat = false " +
            "AND ((m1.user.id = :id1 AND m2.user.id = :id2) OR (m1.user.id = :id2 AND m2.user.id = :id1))")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("id1") Long id1, @Param("id2") Long id2);

    Optional<Chat> findChatById(Long id);

    List<Chat> findByName(String name);
}