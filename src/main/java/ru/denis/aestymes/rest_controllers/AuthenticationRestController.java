package ru.denis.aestymes.rest_controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import ru.denis.aestymes.dtos.LoginForm;
import ru.denis.aestymes.jwts.JwtProvider;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;

@RestController
public class AuthenticationRestController {

    @Autowired
    private MyUserService myUserService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/api/login")
    public RedirectView login(@ModelAttribute LoginForm loginForm, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginForm.email(),
                loginForm.password()
        ));

        if(authentication.isAuthenticated()) {
            String jwtToken = jwtProvider.createToken(myUserService.loadUserByUsername(loginForm.email()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            myUserService.findByEmail(loginForm.email()).ifPresent(user -> {
                myUserService.updateLastSeen(user.getId());
            });

            Cookie cookie = new Cookie("JWT_TOKEN", jwtToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(86400);

            response.addCookie(cookie);

            return new RedirectView("/");
        } else {
            throw new UsernameNotFoundException("Bad credentials");
        }
    }

    @GetMapping("/confirm-account")
    public RedirectView confirm(@RequestParam("token") String token) {
        boolean isConfirm = myUserService.confirmUser(token);
        if(isConfirm) {
            return new RedirectView("/login?verified=true");
        } else {
            return new RedirectView("/login?error=invalid_token");
        }
    }
}