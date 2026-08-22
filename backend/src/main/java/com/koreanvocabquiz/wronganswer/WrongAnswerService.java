package com.koreanvocabquiz.wronganswer;

import java.util.List;
import java.util.Set;

import com.koreanvocabquiz.common.ResourceNotFoundException;
import com.koreanvocabquiz.quiz.QuizGenerationException;
import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.quiz.QuizQuestionResponse;
import com.koreanvocabquiz.quiz.QuizService;
import com.koreanvocabquiz.vocabulary.Vocabulary;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;
import com.koreanvocabquiz.vocabulary.VocabularyRepository;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WrongAnswerService {

    private final WrongAnswerRepository wrongAnswerRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuizService quizService;

    public WrongAnswerService(
            WrongAnswerRepository wrongAnswerRepository,
            VocabularyRepository vocabularyRepository,
            @Lazy QuizService quizService
    ) {
        this.wrongAnswerRepository = wrongAnswerRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.quizService = quizService;
    }

    public List<WrongAnswerResponse> findAll() {
        return wrongAnswerRepository.findAllByOrderByLastWrongAtDesc()
                .stream()
                .map(WrongAnswerResponse::from)
                .toList();
    }

    public List<QuizQuestionResponse> createReviewQuiz(WrongAnswerQuizCreateRequest request) {
        List<Vocabulary> questionVocabularies = wrongAnswerRepository.findAllByOrderByLastWrongAtDesc()
                .stream()
                .filter(wrongAnswer -> wrongAnswer.getQuizMode() == request.mode())
                .filter(wrongAnswer -> request.category() == null
                        || wrongAnswer.getVocabulary().getCategory() == request.category())
                .map(WrongAnswer::getVocabulary)
                .toList();

        if (questionVocabularies.isEmpty()) {
            throw new QuizGenerationException("No wrong answers are available for review.");
        }

        Set<VocabularyCategory> categories = questionVocabularies.stream()
                .map(Vocabulary::getCategory)
                .collect(java.util.stream.Collectors.toSet());
        List<Vocabulary> optionSourceVocabularies = vocabularyRepository.findByCategoryIn(categories);

        int questionCount = request.questionCount() == null ? questionVocabularies.size() : request.questionCount();
        return quizService.createFromVocabularies(
                questionVocabularies,
                optionSourceVocabularies,
                request.mode(),
                questionCount
        );
    }

    @Transactional
    public void recordWrongAnswer(Vocabulary vocabulary, QuizMode quizMode) {
        wrongAnswerRepository.findByVocabularyAndQuizMode(vocabulary, quizMode)
                .ifPresentOrElse(
                        WrongAnswer::increaseWrongCount,
                        () -> wrongAnswerRepository.save(new WrongAnswer(vocabulary, quizMode))
                );
    }

    @Transactional
    public void handleReviewSubmission(Vocabulary vocabulary, QuizMode quizMode, boolean correct) {
        if (correct) {
            wrongAnswerRepository.findByVocabularyAndQuizMode(vocabulary, quizMode)
                    .ifPresent(wrongAnswerRepository::delete);
            return;
        }

        recordWrongAnswer(vocabulary, quizMode);
    }

    @Transactional
    public void delete(Long id) {
        WrongAnswer wrongAnswer = wrongAnswerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wrong answer not found. id=" + id));
        wrongAnswerRepository.delete(wrongAnswer);
    }

    @Transactional
    public void deleteAll() {
        wrongAnswerRepository.deleteAll();
    }

    @Transactional
    public void deleteByVocabulary(Vocabulary vocabulary) {
        wrongAnswerRepository.deleteByVocabulary(vocabulary);
    }
}
