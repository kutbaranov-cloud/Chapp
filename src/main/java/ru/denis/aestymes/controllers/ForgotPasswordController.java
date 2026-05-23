package ru.denis.aestymes.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.denis.aestymes.services.EmailService;
import ru.denis.aestymes.services.MyUserService;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final MyUserService myUserService;
    private final EmailService emailService; // Используем наш сервис
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> resetCodes = new ConcurrentHashMap<>();

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() { return "authentication/forgot-password"; }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        if (myUserService.findByEmail(email).isEmpty()) {
            model.addAttribute("error", "Пользователь не найден");
            return "authentication/forgot-password";
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        resetCodes.put(email, code);

        // Отправка через Resend
        emailService.sendPasswordEmail(email, "Ваш код сброса: " + code);

        model.addAttribute("email", email);
        return "authentication/reset-password-confirm";
    }

    @PostMapping("/reset-password-now")
    public String resetPassword(@RequestParam String email, @RequestParam String code,
                                @RequestParam String newPassword, Model model) {
        String validCode = resetCodes.get(email);
        if (validCode == null || !validCode.equals(code)) {
            model.addAttribute("error", "Неверный код");
            model.addAttribute("email", email);
            return "authentication/reset-password-confirm";
        }
        myUserService.findByEmail(email).ifPresent(user -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            myUserService.saveUserPassword(user);
        });
        resetCodes.remove(email);
        return "redirect:/login?resetSuccess=true";
    }
}