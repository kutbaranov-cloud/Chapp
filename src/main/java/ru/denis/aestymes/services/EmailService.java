package ru.denis.aestymes.services;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
    public void sendConfirmationEmail(String to, String token) {
        // Ничего не делаем, так как статус подтверждается автоматически при регистрации
    }

    public void sendPasswordEmail(String to, String password) {
        System.out.println("DEBUG: Сброс пароля не реализован в текущей версии");
    }
}