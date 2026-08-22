package com.koreanvocabquiz.config;

import com.koreanvocabquiz.admin.AdminAuthenticationInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String allowedOrigin;
    private final AdminAuthenticationInterceptor adminAuthenticationInterceptor;

    public WebConfig(
            @Value("${app.cors.allowed-origin:http://localhost:5173}") String allowedOrigin,
            AdminAuthenticationInterceptor adminAuthenticationInterceptor
    ) {
        this.allowedOrigin = allowedOrigin;
        this.adminAuthenticationInterceptor = adminAuthenticationInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthenticationInterceptor);
    }
}
