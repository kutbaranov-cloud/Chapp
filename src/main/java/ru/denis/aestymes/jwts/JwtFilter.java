package ru.denis.aestymes.jwts;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyUserService;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final MyUserService myUserService;

    public JwtFilter(JwtProvider jwtProvider, @Lazy MyUserService myUserService) {
        this.jwtProvider = jwtProvider;
        this.myUserService = myUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwtToken = getTokenFromRequest(request);

        // ХИРУРГИЧЕСКОЕ ИЗМЕНЕНИЕ: проверяем токен только если аутентификация еще не установлена (например, сессией)
        if (jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtProvider.extractUsername(jwtToken);
                if (username != null) {
                    UserDetails userDetails = myUserService.loadUserByUsername(username);
                    if (jwtProvider.validateToken(jwtToken)) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        myUserService.findByEmail(username).ifPresent(user -> {
                            myUserService.updateLastSeen(user.getId());
                        });
                    }
                }
            } catch (Exception ignored) {
                // Игнорируем ошибки парсинга JWT, чтобы не прерывать запрос и дать шанс сессии
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) return bearer.substring(7);

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }
}