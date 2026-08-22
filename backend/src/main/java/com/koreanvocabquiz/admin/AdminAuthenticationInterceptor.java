package com.koreanvocabquiz.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthenticationInterceptor implements HandlerInterceptor {

    private final AdminAuthenticationService authenticationService;

    public AdminAuthenticationInterceptor(AdminAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !requiresAdminAuthentication(request)) {
            return true;
        }

        // Local tests and the development compose file may omit ADMIN_PASSWORD.
        // Production Compose requires it and therefore always enables this guard.
        if (!authenticationService.isConfigured()) {
            return true;
        }

        if (!authenticationService.isAuthenticated(request.getSession())) {
            throw new AdminAuthenticationException("Admin authentication is required.");
        }

        return true;
    }

    private boolean requiresAdminAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();

        if (path.startsWith("/api/literature/") && !path.startsWith("/api/literature/quizzes")
                && ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))) {
            return true;
        }

        if ("POST".equals(method) && "/api/vocabularies".equals(path)) {
            return true;
        }
        if ("PUT".equals(method) && path.matches("/api/vocabularies/\\d+")) {
            return true;
        }
        if ("DELETE".equals(method) && path.matches("/api/vocabularies/\\d+")) {
            return true;
        }
        return "POST".equals(method) && (
                "/api/vocabularies/csv".equals(path)
                        || "/api/vocabularies/image/extract".equals(path)
                        || "/api/vocabularies/batch".equals(path)
        );
    }
}
