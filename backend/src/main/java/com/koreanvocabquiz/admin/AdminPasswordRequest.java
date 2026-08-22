package com.koreanvocabquiz.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminPasswordRequest(
        @NotBlank
        String password
) {
}
