package ru.denis.aestymes.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.denis.aestymes.models.ChatMember;
import ru.denis.aestymes.models.Message;
import ru.denis.aestymes.models.MyUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    private Long id;
    private String name;
    private Boolean isGroupChat = false;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private String avatarUrl;
    private String member1Username;
    private String member2Username;

}
