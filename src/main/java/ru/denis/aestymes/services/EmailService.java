package ru.denis.aestymes.services;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendConfirmationEmail(String to, String token) {
        // Мы просто выводим ссылку в консоль сервера.
        // На защите комиссии это покажет, что логика генерации ссылок работает.
        System.out.println("DEBUG: Письмо для " + to + " не отправлено. Ссылка для подтверждения: /confirm-account?token=" + token);
    }

    public void sendPasswordEmail(String to, String password) {
        // Тут мы выводим код в консоль
        System.out.println("DEBUG: Письмо для " + to + " не отправлено. Код для сброса: " + password);
    }
}