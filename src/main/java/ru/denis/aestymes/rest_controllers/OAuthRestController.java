package ru.denis.aestymes.rest_controllers;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import ru.denis.aestymes.dtos.LoginForm;
import ru.denis.aestymes.jwts.JwtProvider;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.repositories.MyUserRepository;
import ru.denis.aestymes.services.EmailService;
import ru.denis.aestymes.services.MyUserService;
import ru.denis.aestymes.utils.RandomTextGenerator;


@RestController
public class OAuthRestController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private MyUserService myUserService;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtProvider jwtProvider;

    @GetMapping("/oauth-success")
    public RedirectView oAuthSuccess(@AuthenticationPrincipal OAuth2User principal, HttpServletResponse response) {
        if(principal != null) {
            MyUser user = new MyUser();
            String password = RandomTextGenerator.generateSecureRandomText();

            user.setUsername(principal.getAttribute("sub"));
            user.setEmail(principal.getAttribute("email"));
            user.setName(principal.getAttribute("name"));
            user.setAvatarUrl(principal.getAttribute("picture"));
            user.setPasswordHash(passwordEncoder.encode(password));

            myUserService.save(user);

            emailService.sendPasswordEmail(principal.getAttribute("email"), password);

            // LOGIN
            LoginForm loginForm = new LoginForm(principal.getAttribute("email"), password);

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginForm.email(),
                    loginForm.password()
            ));

            if(authentication.isAuthenticated()) {
                String jwtToken = jwtProvider.createToken(myUserService.loadUserByUsername(loginForm.email()));
                SecurityContextHolder.getContext().setAuthentication(authentication);

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

        return null;
    }
}
