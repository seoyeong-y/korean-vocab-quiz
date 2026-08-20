package com.koreanvocabquiz.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.koreanvocabquiz.common.ResourceNotFoundException;
import com.koreanvocabquiz.vocabulary.Vocabulary;
import com.koreanvocabquiz.vocabulary.VocabularyRepository;
import com.koreanvocabquiz.wronganswer.WrongAnswerService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuizService {

    private static final int OPTION_COUNT = 4;

    private final VocabularyRepository vocabularyRepository;
    private final WrongAnswerService wrongAnswerService;
    private final QuizQuestionSessionStore sessionStore;

    public QuizService(
            VocabularyRepository vocabularyRepository,
            WrongAnswerService wrongAnswerService,
            QuizQuestionSessionStore sessionStore
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.wrongAnswerService = wrongAnswerService;
        this.sessionStore = sessionStore;
    }

    public List<QuizQuestionResponse> create(QuizCreateRequest request) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByCategory(request.category());

        return createFromVocabularies(vocabularies, request.mode(), request.questionCount());
    }

    public List<QuizQuestionResponse> createFromVocabularies(List<Vocabulary> vocabularies, QuizMode mode, int questionCount) {
        return createFromVocabularies(vocabularies, vocabularies, mode, questionCount);
    }

    public List<QuizQuestionResponse> createFromVocabularies(
            List<Vocabulary> questionVocabularies,
            List<Vocabulary> optionSourceVocabularies,
            QuizMode mode,
            int questionCount
    ) {
        if (optionSourceVocabularies.size() < OPTION_COUNT) {
            throw new QuizGenerationException("At least 4 vocabularies are required in the category to create multiple-choice quizzes.");
        }
        if (questionCount > questionVocabularies.size()) {
            throw new QuizGenerationException("questionCount cannot be greater than the number of vocabularies available for this quiz.");
        }

        List<Vocabulary> questions = new ArrayList<>(questionVocabularies);
        Collections.shuffle(questions);

        return questions.stream()
                .limit(questionCount)
                .map(vocabulary -> createQuestion(vocabulary, optionSourceVocabularies, mode))
                .toList();
    }

    @Transactional
    public QuizSubmitResponse submit(QuizSubmitRequest request) {
        QuizQuestionSession session = sessionStore.findValid(request.questionId())
                .orElseThrow(() -> new QuizSubmissionException("Question is not valid or has expired."));

        if (!session.optionVocabularyIds().containsKey(request.selectedOptionId())) {
            throw new QuizSubmissionException("Selected option is not included in the question.");
        }

        Vocabulary correctVocabulary = vocabularyRepository.findById(session.vocabularyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found. id=" + session.vocabularyId()));

        boolean correct = session.correctOptionId().equals(request.selectedOptionId());

        if (request.wrongAnswerReview()) {
            wrongAnswerService.handleReviewSubmission(correctVocabulary, session.mode(), correct);
        } else if (!correct) {
            wrongAnswerService.recordWrongAnswer(correctVocabulary, session.mode());
        }

        return new QuizSubmitResponse(correct, session.correctAnswer(), correctVocabulary.getId());
    }

    private QuizQuestionResponse createQuestion(Vocabulary vocabulary, List<Vocabulary> optionSourceVocabularies, QuizMode mode) {
        List<QuizOptionResponse> options = new ArrayList<>();
        Map<String, Long> optionVocabularyIds = new HashMap<>();

        String correctOptionId = sessionStore.nextId();
        String correctAnswer = answerText(vocabulary, mode);
        options.add(new QuizOptionResponse(correctOptionId, correctAnswer));
        optionVocabularyIds.put(correctOptionId, vocabulary.getId());

        for (Vocabulary distractor : createDistractors(vocabulary, optionSourceVocabularies, mode)) {
            String optionId = sessionStore.nextId();
            options.add(new QuizOptionResponse(optionId, answerText(distractor, mode)));
            optionVocabularyIds.put(optionId, distractor.getId());
        }

        if (options.size() < OPTION_COUNT) {
            throw new QuizGenerationException("At least 4 different option texts are required in the category.");
        }

        Collections.shuffle(options);

        String questionId = sessionStore.nextId();
        sessionStore.save(new QuizQuestionSession(
                questionId,
                vocabulary.getId(),
                mode,
                Map.copyOf(optionVocabularyIds),
                correctOptionId,
                correctAnswer,
                sessionStore.expiresAt()
        ));

        return new QuizQuestionResponse(
                questionId,
                vocabulary.getId(),
                mode,
                questionText(vocabulary, mode),
                options
        );
    }

    private List<Vocabulary> createDistractors(Vocabulary correctVocabulary, List<Vocabulary> categoryVocabularies, QuizMode mode) {
        String correctAnswer = answerText(correctVocabulary, mode);
        List<Vocabulary> candidates = new ArrayList<>(categoryVocabularies);
        Collections.shuffle(candidates);

        Map<String, Vocabulary> distinctCandidates = new LinkedHashMap<>();
        for (Vocabulary candidate : candidates) {
            String answer = answerText(candidate, mode);
            if (candidate.getCategory() == correctVocabulary.getCategory()
                    && !candidate.getId().equals(correctVocabulary.getId())
                    && !answer.equals(correctAnswer)) {
                distinctCandidates.putIfAbsent(answer, candidate);
            }
        }

        return distinctCandidates.values()
                .stream()
                .limit(OPTION_COUNT - 1)
                .toList();
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
