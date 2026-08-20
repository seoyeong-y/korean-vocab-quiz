package com.koreanvocabquiz.common;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        List<String> messages
) {
    public static ApiErrorResponse of(int status, String error, List<String> messages) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, messages);
    }
}
