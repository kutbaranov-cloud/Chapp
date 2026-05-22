package ru.denis.aestymes.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import ru.denis.aestymes.jwts.JwtFilter;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.services.MyOauthUserService;
import ru.denis.aestymes.services.MyUserService;

@Configuration
@EnableWebSecurity
@EnableJdbcHttpSession
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final MyOauthUserService myOauthUserService;
    private final MyUserService myUserService;

    public SecurityConfig(JwtFilter jwtFilter,
                          MyOauthUserService myOauthUserService,
                          @Lazy MyUserService myUserService) {
        this.jwtFilter = jwtFilter;
        this.myOauthUserService = myOauthUserService;
        this.myUserService = myUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password-now", "/css/**", "/js/**", "/confirm-account", "/api/users/ping", "/error", "/api/chats/group").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/chats", true)
                        .failureUrl("/login?error")
                        // ХИРУРГИЧЕСКАЯ ВСТАВКА: Расширенная диагностика
                        .failureHandler((request, response, exception) -> {
                            System.out.println("=== SECURITY DEBUG START ===");
                            System.out.println("Попытка входа с Email: " + request.getParameter("email"));
                            System.out.println("Тип ошибки: " + exception.getClass().getName());
                            System.out.println("Сообщение: " + exception.getMessage());
                            System.out.println("=== SECURITY DEBUG END ===");
                            response.sendRedirect("/login?error");
                        })
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("aesty_fixed_secret_key_999")
                        .tokenValiditySeconds(604800)
                        .rememberMeParameter("remember-me")
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(myOauthUserService))
                        .defaultSuccessUrl("/chats", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler((request, response, authentication) -> {
                            MyUser user = myUserService.getAuthenticatedUser(request);
                            if (user != null) {
                                myUserService.setOffline(user.getId());
                            }
                        })
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JWT_TOKEN", "JSESSIONID", "remember-me")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}