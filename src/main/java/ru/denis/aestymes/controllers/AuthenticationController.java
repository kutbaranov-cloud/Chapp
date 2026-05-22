package ru.denis.aestymes.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.denis.aestymes.dtos.LoginForm;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;

@Controller
public class AuthenticationController {

    @Autowired
    @Lazy
    private MyUserService myUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String root() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return "redirect:/chats";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model, LoginForm loginForm) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return "redirect:/chats";
        }
        model.addAttribute("loginForm", loginForm);
        return "authentication/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return "redirect:/chats";
        }
        return "authentication/register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute MyUser user, RedirectAttributes redirectAttributes) {
        try {
            // Линия защиты 1: Шифруем пароль
            if (user.getPasswordHash() != null) {
                user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            }

            myUserService.save(user);
            redirectAttributes.addFlashAttribute("registrationSuccess", true);
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при регистрации: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        MyUser user = myUserService.getAuthenticatedUser(request);
        if (user != null) {
            myUserService.setOffline(user.getId());
        }
        SecurityContextHolder.clearContext();
        Cookie cookie = new Cookie("JWT_TOKEN", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login?logout";
    }
}