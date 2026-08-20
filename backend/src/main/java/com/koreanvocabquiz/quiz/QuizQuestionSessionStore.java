package com.koreanvocabquiz.quiz;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class QuizQuestionSessionStore {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final ConcurrentMap<String, QuizQuestionSession> sessions = new ConcurrentHashMap<>();

    public String nextId() {
        return UUID.randomUUID().toString();
    }

    public void save(QuizQuestionSession session) {
        cleanupExpired();
        sessions.put(session.questionId(), session);
    }

    public Optional<QuizQuestionSession> findValid(String questionId) {
        cleanupExpired();
        QuizQuestionSession session = sessions.get(questionId);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired(LocalDateTime.now())) {
            sessions.remove(questionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public LocalDateTime expiresAt() {
        return LocalDateTime.now().plus(SESSION_TTL);
    }

    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
}
