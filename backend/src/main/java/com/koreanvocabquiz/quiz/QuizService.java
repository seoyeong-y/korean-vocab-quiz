package com.koreanvocabquiz.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.koreanvocabquiz.common.ResourceNotFoundException;
import com.koreanvocabquiz.learning.VocabularyLearningProgress;
import com.koreanvocabquiz.learning.VocabularyLearningProgressRepository;
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
    private final MasteredVocabularyRepository masteredVocabularyRepository;
    private final VocabularyLearningProgressRepository learningProgressRepository;

    public QuizService(
            VocabularyRepository vocabularyRepository,
            WrongAnswerService wrongAnswerService,
            QuizQuestionSessionStore sessionStore,
            MasteredVocabularyRepository masteredVocabularyRepository,
            VocabularyLearningProgressRepository learningProgressRepository
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.wrongAnswerService = wrongAnswerService;
        this.sessionStore = sessionStore;
        this.masteredVocabularyRepository = masteredVocabularyRepository;
        this.learningProgressRepository = learningProgressRepository;
    }

    public List<QuizQuestionResponse> create(QuizCreateRequest request) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByCategory(request.category());
        Set<Long> masteredVocabularyIds = masteredVocabularyRepository.findMasteredVocabularyIds();
        List<Vocabulary> quizVocabularies = vocabularies.stream()
                .filter(vocabulary -> !masteredVocabularyIds.contains(vocabulary.getId()))
                .toList();
        List<Vocabulary> prioritizedVocabularies = prioritizeByAttemptCount(quizVocabularies);

        return createFromVocabularies(prioritizedVocabularies, prioritizedVocabularies, request.mode(), request.questionCount(), false);
    }

    public List<QuizQuestionResponse> createFromVocabularies(List<Vocabulary> vocabularies, QuizMode mode, int questionCount) {
        return createFromVocabularies(vocabularies, vocabularies, mode, questionCount, true);
    }

    public List<QuizQuestionResponse> createFromVocabularies(
            List<Vocabulary> questionVocabularies,
            List<Vocabulary> optionSourceVocabularies,
            QuizMode mode,
            int questionCount
    ) {
        return createFromVocabularies(questionVocabularies, optionSourceVocabularies, mode, questionCount, true);
    }

    private List<QuizQuestionResponse> createFromVocabularies(
            List<Vocabulary> questionVocabularies,
            List<Vocabulary> optionSourceVocabularies,
            QuizMode mode,
            int questionCount,
            boolean shuffleQuestions
    ) {
        if (optionSourceVocabularies.size() < OPTION_COUNT) {
            throw new QuizGenerationException("At least 4 vocabularies are required in the category to create multiple-choice quizzes.");
        }
        if (questionCount > questionVocabularies.size()) {
            throw new QuizGenerationException("questionCount cannot be greater than the number of vocabularies available for this quiz.");
        }

        List<Vocabulary> questions = new ArrayList<>(questionVocabularies);
        if (shuffleQuestions) {
            Collections.shuffle(questions);
        }

        return questions.stream()
                .limit(questionCount)
                .map(vocabulary -> createQuestion(vocabulary, optionSourceVocabularies, resolveMode(mode)))
                .toList();
    }

    private List<Vocabulary> prioritizeByAttemptCount(List<Vocabulary> vocabularies) {
        Map<Long, Integer> attemptCounts = learningProgressRepository.findByVocabularyIdIn(
                        vocabularies.stream().map(Vocabulary::getId).toList()
                )
                .stream()
                .collect(Collectors.toMap(
                        progress -> progress.getVocabulary().getId(),
                        VocabularyLearningProgress::getAttemptCount
                ));

        return vocabularies.stream()
                .collect(Collectors.groupingBy(
                        vocabulary -> attemptCounts.getOrDefault(vocabulary.getId(), 0),
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> {
                    List<Vocabulary> group = entry.getValue();
                    Collections.shuffle(group);
                    return group.stream();
                })
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
        sessionStore.recordSubmissionResult(new QuizQuestionSubmissionResult(
                session.questionId(),
                correctVocabulary.getId(),
                correctVocabulary.getCategory(),
                session.mode(),
                correct,
                false
        ));

        return new QuizSubmitResponse(correct, session.correctAnswer(), correctVocabulary.getId());
    }

    @Transactional
    public QuizMasteredResponse markMastered(QuizMasteredRequest request) {
        QuizQuestionSession session = sessionStore.findValid(request.questionId())
                .orElseThrow(() -> new QuizSubmissionException("Question is not valid or has expired."));

        Vocabulary vocabulary = vocabularyRepository.findById(session.vocabularyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found. id=" + session.vocabularyId()));

        if (!masteredVocabularyRepository.existsByVocabulary(vocabulary)) {
            masteredVocabularyRepository.save(new MasteredVocabulary(vocabulary));
        }
        wrongAnswerService.deleteByVocabulary(vocabulary);
        if (sessionStore.findSubmissionResult(session.questionId()).isEmpty()) {
            sessionStore.recordSubmissionResult(new QuizQuestionSubmissionResult(
                    session.questionId(),
                    vocabulary.getId(),
                    vocabulary.getCategory(),
                    session.mode(),
                    true,
                    true
            ));
        }

        return new QuizMasteredResponse(true, session.correctAnswer(), vocabulary.getId());
    }

    @Transactional
    public QuizMasteredResponse unmarkMastered(String questionId) {
        QuizQuestionSession session = sessionStore.findValid(questionId)
                .orElseThrow(() -> new QuizSubmissionException("Question is not valid or has expired."));

        Vocabulary vocabulary = vocabularyRepository.findById(session.vocabularyId())
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found. id=" + session.vocabularyId()));

        masteredVocabularyRepository.deleteByVocabulary(vocabulary);
        sessionStore.findSubmissionResult(questionId)
                .filter(QuizQuestionSubmissionResult::mastered)
                .ifPresent(result -> sessionStore.removeSubmissionResult(questionId));

        return new QuizMasteredResponse(false, session.correctAnswer(), vocabulary.getId());
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

    private QuizMode resolveMode(QuizMode mode) {
        if (mode != QuizMode.MIXED) {
            return mode;
        }

        return ThreadLocalRandom.current().nextBoolean()
                ? QuizMode.WORD_TO_MEANING
                : QuizMode.MEANING_TO_WORD;
    }

    private String questionText(Vocabulary vocabulary, QuizMode mode) {
        return switch (mode) {
            case WORD_TO_MEANING -> vocabulary.getWord();
            case MEANING_TO_WORD -> vocabulary.getMeaning();
            case MIXED -> throw new IllegalArgumentException("MIXED mode must be resolved before creating a question.");
        };
    }

    private String answerText(Vocabulary vocabulary, QuizMode mode) {
        return switch (mode) {
            case WORD_TO_MEANING -> vocabulary.getMeaning();
            case MEANING_TO_WORD -> vocabulary.getWord();
            case MIXED -> throw new IllegalArgumentException("MIXED mode must be resolved before creating options.");
        };
    }
}
