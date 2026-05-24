package ru.denis.aestymes.configs; // Или ru.denis.aestymes.config

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Это говорит Spring: если видишь URL /uploads/...,
        // ищи файл в папке uploads/ в корне твоего проекта.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}