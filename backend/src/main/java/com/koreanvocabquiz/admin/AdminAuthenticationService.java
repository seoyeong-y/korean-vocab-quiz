package com.koreanvocabquiz.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthenticationService {

    private static final String SESSION_ATTRIBUTE = "KOREAN_VOCAB_ADMIN_AUTHENTICATED";

    private final String adminPassword;

    public AdminAuthenticationService(@Value("${app.admin-password:}") String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public boolean authenticate(String password) {
        if (adminPassword.isBlank() || password == null) {
            return false;
        }

        return MessageDigest.isEqual(
                adminPassword.getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8)
        );
    }

    public boolean isConfigured() {
        return !adminPassword.isBlank();
    }

    public void markAuthenticated(jakarta.servlet.http.HttpSession session) {
        session.setAttribute(SESSION_ATTRIBUTE, Boolean.TRUE);
    }

    public boolean isAuthenticated(jakarta.servlet.http.HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SESSION_ATTRIBUTE));
    }

    public void clear(jakarta.servlet.http.HttpSession session) {
        session.removeAttribute(SESSION_ATTRIBUTE);
    }
}
