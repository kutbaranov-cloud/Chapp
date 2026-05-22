package ru.denis.aestymes.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.denis.aestymes.models.MessageRead;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {
}
