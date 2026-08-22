package com.koreanvocabquiz.literature;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class LiteraryQuizQuestionSessionStore {
    private static final Duration TTL = Duration.ofMinutes(30);
    private final ConcurrentMap<String, LiteraryQuizQuestionSession> sessions = new ConcurrentHashMap<>();

    public String nextId() { return UUID.randomUUID().toString(); }
    public LocalDateTime expiresAt() { return LocalDateTime.now().plus(TTL); }
    public void save(LiteraryQuizQuestionSession session) { cleanup(); sessions.put(session.questionId(), session); }
    public Optional<LiteraryQuizQuestionSession> findValid(String questionId) {
        cleanup();
        LiteraryQuizQuestionSession session = sessions.get(questionId);
        if (session == null || session.isExpired(LocalDateTime.now())) {
            sessions.remove(questionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }
    private void cleanup() { LocalDateTime now = LocalDateTime.now(); sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now)); }
}
