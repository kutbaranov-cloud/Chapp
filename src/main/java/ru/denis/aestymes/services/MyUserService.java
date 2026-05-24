package ru.denis.aestymes.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j; // Добавлено для логов
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile; // Добавлено для файлов
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import ru.denis.aestymes.dtos.ChangePasswordDto;
import ru.denis.aestymes.dtos.UserDTO;
import ru.denis.aestymes.jwts.JwtProvider;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.repositories.MyUserRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j // Включает log.error и другие методы логгирования
@Service
public class MyUserService implements UserDetailsService {

    @Autowired
    private MyUserRepository myUserRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    @Lazy
    private SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> disconnectTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Data
    @AllArgsConstructor
    public static class UserStatusPayload {
        private Long userId;
        private boolean online;
        private String lastSeenText;
    }

    @Transactional
    public boolean confirmUser(String token) {
        MyUser user = myUserRepository.findByConfirmationToken(token);
        if (user != null) {
            user.setVerified(true);
            user.setConfirmationToken(null);
            myUserRepository.save(user);
            return true;
        }
        return false;
    }

    public String formatLastSeen(LocalDateTime lastSeen) {
        if (lastSeen == null) return "недавно";
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(lastSeen, now);
        long seconds = duration.getSeconds();

        if (seconds < 60) return "только что";
        long minutes = duration.toMinutes();
        if (minutes < 60) return minutes + " мин. назад";

        if (lastSeen.toLocalDate().equals(now.toLocalDate())) {
            return "сегодня в " + lastSeen.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (lastSeen.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
            return "вчера в " + lastSeen.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            return lastSeen.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }
    }

    @Transactional
    public void updateLastSeen(Long userId) {
        ScheduledFuture<?> scheduledTask = disconnectTasks.remove(userId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }

        MyUser user = myUserRepository.getMyUserById(userId);
        if (user != null) {
            user.setLastSeen(LocalDateTime.now());
            if (!"online".equals(user.getStatus())) {
                user.setStatus("online");
                myUserRepository.saveAndFlush(user);
                messagingTemplate.convertAndSend("/topic/user-status",
                        new UserStatusPayload(userId, true, "online"));
            }
        }
    }

    @Transactional
    public void setOffline(Long userId) {
        MyUser user = myUserRepository.getMyUserById(userId);
        if (user != null && "online".equals(user.getStatus())) {
            user.setLastSeen(LocalDateTime.now());
            user.setStatus("offline");
            user.setLastLogout(LocalDateTime.now());
            myUserRepository.saveAndFlush(user);

            String timeText = formatLastSeen(user.getLastSeen());
            messagingTemplate.convertAndSend("/topic/user-status",
                    new UserStatusPayload(userId, false, timeText));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) headerAccessor.getUser();

        if (auth != null) {
            findByEmail(auth.getName()).ifPresent(user -> {
                ScheduledFuture<?> task = scheduler.schedule(() -> {
                    setOffline(user.getId());
                    disconnectTasks.remove(user.getId());
                }, 5, TimeUnit.SECONDS);

                disconnectTasks.put(user.getId(), task);
            });
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        MyUser user = myUserRepository.findMyUserByEmail(email);
        if(user != null) {
            return User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .disabled(!user.isVerified())
                    .authorities("ROLE_USER")
                    .build();
        }
        throw new UsernameNotFoundException("User not found: " + email);
    }

    public UserDTO convertToDTO(MyUser user) {
        if (user == null) return null;
        return new UserDTO(user.getId(), user.getUsername(), user.getName(), user.getAvatarUrl(), "online".equals(user.getStatus()));
    }

    public MyUser getAuthenticatedUser(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return this.findByEmail(auth.getName()).orElse(null);
        }

        Long currentUserId = this.getCurrentUserId(request);
        if (currentUserId != null && currentUserId != -1L) {
            return this.getUserById(currentUserId);
        }
        return null;
    }

    public Optional<MyUser> findByEmail(String email) { return Optional.ofNullable(myUserRepository.findMyUserByEmail(email)); }
    public Optional<MyUser> findByUsername(String username) { return myUserRepository.findByUsername(username); }
    public MyUser getUserById(Long id) { return myUserRepository.getMyUserById(id); }

    public Optional<MyUser> findById(Long id) {
        return Optional.ofNullable(myUserRepository.getMyUserById(id));
    }

    public void saveUserPassword(MyUser user) {
        myUserRepository.save(user);
    }

    public void save(MyUser myUser) {
        if (myUser.getPasswordHash() != null && !myUser.getPasswordHash().startsWith("$2a$")) {
            myUser.setPasswordHash(passwordEncoder.encode(myUser.getPasswordHash()));
        }
        myUser.setVerified(false);
        String confirmationToken = UUID.randomUUID().toString();
        myUser.setConfirmationToken(confirmationToken);
        myUserRepository.save(myUser);
        emailService.sendConfirmationEmail(myUser.getEmail(), confirmationToken);
    }

    public Long getCurrentUserId(HttpServletRequest request) {
        if (request.getCookies() == null) return -1L;
        return Arrays.stream(request.getCookies())
                .filter(c -> "JWT_TOKEN".equals(c.getName()))
                .findFirst()
                .map(c -> {
                    try {
                        String email = jwtProvider.extractUsername(c.getValue());
                        MyUser u = myUserRepository.findMyUserByEmail(email);
                        return u != null ? u.getId() : -1L;
                    } catch (Exception e) { return -1L; }
                }).orElse(-1L);
    }

    public void changePassword(ChangePasswordDto passwordDto, MyUser user) {
        if(!passwordEncoder.matches(passwordDto.getOldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Wrong old password");
        }
        user.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));
        myUserRepository.save(user);
    }

    // ИСПРАВЛЕННЫЙ МЕТОД ОБНОВЛЕНИЯ ПРОФИЛЯ
    // ИСПРАВЛЕННЫЙ МЕТОД ОБНОВЛЕНИЯ ПРОФИЛЯ
    @Transactional
    public void updateProfile(Long user_id, UserDTO dto, MultipartFile file) {
        MyUser user = myUserRepository.getMyUserById(user_id);
        if(user != null) {
            // 1. Обновляем текстовые данные
            user.setName(dto.getName());
            user.setUsername(dto.getUsername());

            // 2. Если пришел файл - сохраняем его на диск
            if (file != null && !file.isEmpty()) {
                try {
                    String uploadDir = "uploads/";
                    File dir = new File(uploadDir);
                    if (!dir.exists()) dir.mkdirs();

                    String fileName = "user_avatar_" + user.getId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path path = Paths.get(uploadDir + fileName);
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                    user.setAvatarUrl("/uploads/" + fileName);
                } catch (Exception e) {
                    log.error("Ошибка при сохранении аватара", e);
                }
            }
            // 3. ИНАЧЕ, если файла нет, но пришла текстовая ссылка из DTO (например, готовые аватары DiceBear) - сохраняем её!
            // 3. ИНАЧЕ, если файла нет, но пришла текстовая ссылка из DTO (например, готовые аватары DiceBear) - сохраняем её!
            else if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isEmpty()) {
                user.setAvatarUrl(dto.getAvatarUrl());
            }

            myUserRepository.save(user);

            // РАССЫЛАЕМ ОБНОВЛЕНИЕ ПРОФИЛЯ ВСЕМ ОНЛАЙН-ПОЛЬЗОВАТЕЛЯМ
            java.util.Map<String, Object> updatePayload = new java.util.HashMap<>();
            updatePayload.put("type", "USER_PROFILE_UPDATE");
            updatePayload.put("userId", user.getId());
            updatePayload.put("newName", user.getName());
            updatePayload.put("newAvatar", user.getAvatarUrl());

            messagingTemplate.convertAndSend("/topic/user-status", updatePayload);
        }
    }

//    @jakarta.annotation.PostConstruct
//    public void initTestUser() {
//        try {
//            MyUser user = myUserRepository.findMyUserByEmail("evabuldogova@bk.ru");
//            if (user != null) {
//                user.setPasswordHash(passwordEncoder.encode("12345"));
//                user.setVerified(true);
//                myUserRepository.save(user);
//                System.out.println("=== TEST USER UPDATED SUCCESSFULLY (Email: evabuldogova@bk.ru, Pass: 12345) ===");
//            }
//        } catch (Exception e) {
//            System.out.println("=== TEST USER UPDATE FAILED: " + e.getMessage() + " ===");
//        }
//    }
}