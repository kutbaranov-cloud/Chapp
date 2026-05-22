package ru.denis.aestymes.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController {

    private final MyUserService myUserService;

    @PostMapping("/ping")
    public void ping(HttpServletRequest request) {
        MyUser currentUser = myUserService.getAuthenticatedUser(request);
        if (currentUser != null) {
            myUserService.updateLastSeen(currentUser.getId());
        }
    }
}