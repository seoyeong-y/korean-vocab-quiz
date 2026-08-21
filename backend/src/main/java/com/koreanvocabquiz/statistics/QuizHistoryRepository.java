package com.koreanvocabquiz.statistics;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {

    List<QuizHistory> findAllByOrderByCompletedAtDesc();

    List<QuizHistory> findTop10ByOrderByCompletedAtDesc();
}
