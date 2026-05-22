package ru.denis.aestymes.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;

@RestController
@RequestMapping("/api/status") // ИЗМЕНЕНО: был /api/users, теперь /api/status
public class UserStatusController {

    @Autowired
    private MyUserService myUserService;

    @PostMapping("/ping")
    public ResponseEntity<Void> ping(HttpServletRequest request) {
        MyUser user = myUserService.getAuthenticatedUser(request);
        if (user != null) {
            myUserService.updateLastSeen(user.getId());
        }
        return ResponseEntity.ok().build();
    }
}