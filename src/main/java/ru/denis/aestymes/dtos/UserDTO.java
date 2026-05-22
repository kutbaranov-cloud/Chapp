package ru.denis.aestymes.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String name;
    private String avatarUrl;
    private boolean online; // ВНЕДРЕНО: Статус пользователя
}