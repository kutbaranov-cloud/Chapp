package ru.denis.aestymes.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;
import ru.denis.aestymes.dtos.ChangePasswordDto;
import ru.denis.aestymes.dtos.UserDTO;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private MyUserService myUserService;

    @GetMapping("/edit")
    public String editProfilePage(Model model, HttpServletRequest request) {
        MyUser user = myUserService.getAuthenticatedUser(request);

        if (user == null) {
            return "redirect:/login";
        }

        // Вычисляем статус онлайн для DTO (5-й аргумент)
        boolean isOnline = user.getLastSeen() != null &&
                user.getLastSeen().isAfter(LocalDateTime.now().minusMinutes(5));

        model.addAttribute("user", user);
        // ИСПРАВЛЕНО: Теперь передаем 5 аргументов в конструктор UserDTO
        model.addAttribute("userDTO", new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getAvatarUrl(),
                isOnline
        ));

        return "profile/edit";
    }

    @PostMapping("/edit/save")
    public String saveProfile(@ModelAttribute("userDTO") UserDTO userDTO, BindingResult result, HttpServletRequest request) {
        if(result.hasErrors()) {
            return "profile/edit";
        }

        MyUser currentUser = myUserService.getAuthenticatedUser(request);
        if (currentUser == null) return "redirect:/login";

        try {
            myUserService.updateProfile(currentUser.getId(), userDTO);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/chats";
    }

    @GetMapping("/password/edit")
    public String passwordEditPage(Model model) {
        model.addAttribute("passwordDto", new ChangePasswordDto());
        return "profile/editPassword";
    }

    @PostMapping("/password/edit/save")
    public RedirectView passwordEdit(@ModelAttribute ChangePasswordDto passwordDto, HttpServletRequest request) {
        MyUser currentUser = myUserService.getAuthenticatedUser(request);

        if(passwordDto != null && currentUser != null) {
            myUserService.changePassword(passwordDto, currentUser);
            return new RedirectView("/chats");
        } else {
            throw new BadCredentialsException("Ошибка доступа или пустые данные пароля");
        }
    }
}