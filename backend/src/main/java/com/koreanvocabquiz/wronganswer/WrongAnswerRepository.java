package com.koreanvocabquiz.wronganswer;

import java.util.List;
import java.util.Optional;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.vocabulary.Vocabulary;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WrongAnswerRepository extends JpaRepository<WrongAnswer, Long> {

    @EntityGraph(attributePaths = "vocabulary")
    List<WrongAnswer> findAllByOrderByLastWrongAtDesc();

    @EntityGraph(attributePaths = "vocabulary")
    List<WrongAnswer> findTop5ByOrderByWrongCountDescLastWrongAtDesc();

    Optional<WrongAnswer> findByVocabularyAndQuizMode(Vocabulary vocabulary, QuizMode quizMode);

    void deleteByVocabulary(Vocabulary vocabulary);
}
