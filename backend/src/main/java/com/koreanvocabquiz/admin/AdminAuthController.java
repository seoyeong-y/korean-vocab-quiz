package com.koreanvocabquiz.admin;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthenticationService authenticationService;

    public AdminAuthController(AdminAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public AdminAuthResponse status(HttpSession session) {
        return new AdminAuthResponse(authenticationService.isAuthenticated(session));
    }

    @PostMapping
    public AdminAuthResponse authenticate(
            @Valid @RequestBody AdminPasswordRequest request,
            HttpSession session
    ) {
        if (!authenticationService.authenticate(request.password())) {
            throw new AdminAuthenticationException("Invalid admin password.");
        }

        authenticationService.markAuthenticated(session);
        return new AdminAuthResponse(true);
    }

    @DeleteMapping
    public ResponseEntity<Void> logout(HttpSession session) {
        authenticationService.clear(session);
        return ResponseEntity.noContent().build();
    }
}
