package com.koreanvocabquiz.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.koreanvocabquiz.common.ResourceNotFoundException;
import com.koreanvocabquiz.vocabulary.Vocabulary;
import com.koreanvocabquiz.vocabulary.VocabularyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuizService {

    private static final int OPTION_COUNT = 4;

    private final VocabularyRepository vocabularyRepository;

    public QuizService(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    public List<QuizQuestionResponse> create(QuizCreateRequest request) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByCategory(request.category());

        if (vocabularies.size() < OPTION_COUNT) {
            throw new QuizGenerationException("At least 4 vocabularies are required in the category to create multiple-choice quizzes.");
        }
        if (request.questionCount() > vocabularies.size()) {
            throw new QuizGenerationException("questionCount cannot be greater than the number of vocabularies in the category.");
        }
        if (distinctAnswerCount(vocabularies, request.mode()) < OPTION_COUNT) {
            throw new QuizGenerationException("At least 4 different option texts are required in the category.");
        }

        List<Vocabulary> questions = new ArrayList<>(vocabularies);
        Collections.shuffle(questions);

        return questions.stream()
                .limit(request.questionCount())
                .map(vocabulary -> createQuestion(vocabulary, vocabularies, request.mode()))
                .toList();
    }

    public QuizSubmitResponse submit(QuizSubmitRequest request) {
        Vocabulary correctVocabulary = vocabularyRepository.findById(request.vocabularyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found. id=" + request.vocabularyId()));
        Vocabulary selectedVocabulary = vocabularyRepository.findById(request.selectedOptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected option not found. id=" + request.selectedOptionId()));

        String correctAnswer = answerText(correctVocabulary, request.mode());
        boolean correct = correctAnswer.equals(answerText(selectedVocabulary, request.mode()));

        return new QuizSubmitResponse(correct, correctAnswer, correctVocabulary.getId());
    }

    private QuizQuestionResponse createQuestion(Vocabulary vocabulary, List<Vocabulary> categoryVocabularies, QuizMode mode) {
        List<QuizOptionResponse> options = new ArrayList<>();
        options.add(new QuizOptionResponse(vocabulary.getId(), answerText(vocabulary, mode)));
        options.addAll(createDistractors(vocabulary, categoryVocabularies, mode));
        Collections.shuffle(options);

        return new QuizQuestionResponse(
                vocabulary.getId(),
                mode,
                questionText(vocabulary, mode),
                options
        );
    }

    private List<QuizOptionResponse> createDistractors(Vocabulary correctVocabulary, List<Vocabulary> categoryVocabularies, QuizMode mode) {
        String correctAnswer = answerText(correctVocabulary, mode);
        List<Vocabulary> candidates = new ArrayList<>(categoryVocabularies);
        Collections.shuffle(candidates);

        Map<String, Vocabulary> distinctCandidates = new LinkedHashMap<>();
        for (Vocabulary candidate : candidates) {
            String answer = answerText(candidate, mode);
            if (!candidate.getId().equals(correctVocabulary.getId()) && !answer.equals(correctAnswer)) {
                distinctCandidates.putIfAbsent(answer, candidate);
            }
        }

        return distinctCandidates.values()
                .stream()
                .limit(OPTION_COUNT - 1)
                .map(candidate -> new QuizOptionResponse(candidate.getId(), answerText(candidate, mode)))
                .toList();
    }

    private long distinctAnswerCount(List<Vocabulary> vocabularies, QuizMode mode) {
        return vocabularies.stream()
                .map(vocabulary -> answerText(vocabulary, mode))
                .distinct()
                .count();
    }

    private String questionText(Vocabulary vocabulary, QuizMode mode) {
        return switch (mode) {
            case WORD_TO_MEANING -> vocabulary.getWord();
            case MEANING_TO_WORD -> vocabulary.getMeaning();
        };
    }

    private String answerText(Vocabulary vocabulary, QuizMode mode) {
        return switch (mode) {
            case WORD_TO_MEANING -> vocabulary.getMeaning();
            case MEANING_TO_WORD -> vocabulary.getWord();
        };
    }
}
