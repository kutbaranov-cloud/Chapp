package ru.denis.aestymes.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.denis.aestymes.models.MyUser;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {

    MyUser findByEmail(String email);

    MyUser findMyUserByEmail(String email);

    MyUser getMyUserById(Long id);

    MyUser findByConfirmationToken(String confirmationToken);

    Optional<MyUser> findByUsername(String username);

    MyUser findMyUserByUsername(String username);

    MyUser getMyUserByUsername(String username);

    // ВНЕДРЕНИЕ: Нативный запрос для мгновенного обновления статуса
    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET last_seen = :lastSeen WHERE id = :userId", nativeQuery = true)
    void updateLastSeenNative(@Param("userId") Long userId, @Param("lastSeen") LocalDateTime lastSeen);
}