package ru.denis.aestymes.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${app.email.url}")
    private String emailUrl;

    // Метод, который искал Спринг
    public void sendConfirmationEmail(String to, String token) {
        String confirmationLink = emailUrl + "?token=" + token;
        sendEmail(to, "Подтверждение регистрации", "Для подтверждения перейдите по ссылке: " + confirmationLink);
    }

    // Метод для забытого пароля
    public void sendPasswordEmail(String to, String password) {
        sendEmail(to, "Сброс пароля", "Ваш код для сброса: " + password);
    }

    // Универсальный метод отправки (внутренний)
    private void sendEmail(String to, String subject, String content) {
        String url = "https://api.resend.com/emails";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "from", "onboarding@resend.dev",
                "to", to,
                "subject", subject,
                "html", "<p>" + content + "</p>"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForObject(url, request, String.class);
    }
}