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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;
import ru.denis.aestymes.dtos.ChangePasswordDto;
import ru.denis.aestymes.dtos.UserDTO;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private MyUserService myUserService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // ХИРУРГИЧЕСКИ ДОБАВЛЕНО: Шаблон для real-time уведомлений

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
    @ResponseBody // Добавляем аннотацию, чтобы возвращать JSON
    public ResponseEntity<?> saveProfile(@ModelAttribute("userDTO") UserDTO userDTO,
                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                         HttpServletRequest request) {
        MyUser currentUser = myUserService.getAuthenticatedUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Не авторизован");
        }

        try {
            myUserService.updateProfile(currentUser.getId(), userDTO, file);

            // ХИРУРГИЧЕСКИ ДОБАВЛЕНО: Получаем объект из БД с уже сгенерированным новым URL аватарки
            MyUser updatedUser = myUserService.getUserById(currentUser.getId());

            // Формируем payload для отправки в WebSocket топик всем клиентам
            java.util.Map<String, Object> updateData = new java.util.HashMap<>();
            updateData.put("type", "USER_PROFILE_UPDATE");
            updateData.put("userId", updatedUser.getId());
            updateData.put("newName", updatedUser.getName());
            updateData.put("newAvatarUrl", updatedUser.getAvatarUrl());

            // Публикуем событие обновления профиля в глобальный топик
            messagingTemplate.convertAndSend("/topic/global-updates", updateData);

            return ResponseEntity.ok(java.util.Map.of("status", "success"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ошибка при сохранении профиля");
        }
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