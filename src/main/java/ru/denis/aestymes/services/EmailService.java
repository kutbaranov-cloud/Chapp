package ru.denis.aestymes.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${app.email.url}")
    private String emailUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private JavaMailSender mailSender;

    public void sendConfirmationEmail(String to, String token) {
        String confirmationLink = emailUrl + "?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setFrom(fromEmail); // Берет kutbaranov@gmail.com
        message.setSubject("Aesty messenger register confirmation");
        message.setText("To complete the registration, please click the link below " + confirmationLink);

        mailSender.send(message);
    }

    public void sendPasswordEmail(String to, String password) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setFrom(fromEmail); // Берет kutbaranov@gmail.com
        message.setSubject("Aesty messenger register password");
        message.setText("Your password for aesty messenger: " + password);

        mailSender.send(message);
    }
}