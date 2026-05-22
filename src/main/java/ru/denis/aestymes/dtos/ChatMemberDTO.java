package ru.denis.aestymes.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemberDTO {
    private Long id;
    private UserDTO user;
    // НЕ включаем обратную ссылку на Chat!

    // конструкторы, геттеры, сеттеры
}
