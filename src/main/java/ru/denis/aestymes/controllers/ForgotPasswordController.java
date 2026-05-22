package ru.denis.aestymes.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // ДОБАВЛЕНО
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final MyUserService myUserService;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    // ХИРУРГИЧЕСКИ: Берем почту отправителя из настроек проекта автоматически
    @Value("${spring.mail.username}")
    private String fromEmail;

    // Используем ConcurrentHashMap для потокобезопасности
    private final Map<String, String> resetCodes = new ConcurrentHashMap<>();

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "authentication/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        var userOpt = myUserService.findByEmail(email);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Пользователь с такой почтой не найден");
            return "authentication/forgot-password";
        }

        // 1. Генерируем код
        String code = String.format("%06d", new Random().nextInt(999999));
        resetCodes.put(email, code);

        // 2. Отправка письма
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // ИСПРАВЛЕНО: Теперь берет kutbaranov@gmail.com из конфига
            message.setTo(email);
            message.setSubject("Код восстановления пароля - Aesty Messenger");
            message.setText("Ваш код для сброса пароля: " + code);
            mailSender.send(message);
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при отправке письма: " + e.getMessage());
            return "authentication/forgot-password";
        }

        model.addAttribute("email", email);
        return "authentication/reset-password-confirm";
    }

    @PostMapping("/reset-password-now")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String code,
                                @RequestParam String newPassword,
                                Model model) {
        String validCode = resetCodes.get(email);

        if (validCode == null || !validCode.equals(code)) {
            model.addAttribute("error", "Неверный или просроченный код");
            model.addAttribute("email", email);
            return "authentication/reset-password-confirm";
        }

        // Обновляем пароль через сервис
        myUserService.findByEmail(email).ifPresent(user -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            myUserService.saveUserPassword(user);
        });

        resetCodes.remove(email);
        return "redirect:/login?resetSuccess=true";
    }
}