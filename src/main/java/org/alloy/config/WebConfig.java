package org.alloy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                    "http://*", 
                    "https://*",
                    "http://localhost:*",
                    "http://192.168.*:*",
                    "http://95.172.*:*",
                    "http://5.227.*:*",
                    "http://alloynn.keenetic.name:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // Включаем credentials для JWT токенов
                .maxAge(3600);
    }
} 