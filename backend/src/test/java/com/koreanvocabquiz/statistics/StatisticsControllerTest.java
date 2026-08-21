package com.koreanvocabquiz.statistics;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.quiz.QuizQuestionSession;
import com.koreanvocabquiz.quiz.QuizQuestionSessionStore;
import com.koreanvocabquiz.vocabulary.Vocabulary;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;
import com.koreanvocabquiz.vocabulary.VocabularyRepository;
import com.koreanvocabquiz.wronganswer.WrongAnswer;
import com.koreanvocabquiz.wronganswer.WrongAnswerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statistics-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private WrongAnswerRepository wrongAnswerRepository;

    @Autowired
    private QuizHistoryRepository quizHistoryRepository;

    @Autowired
    private QuizQuestionSessionStore sessionStore;

    private Vocabulary apple;
    private Vocabulary banana;
    private Vocabulary grape;

    @BeforeEach
    void setUp() {
        quizHistoryRepository.deleteAll();
        wrongAnswerRepository.deleteAll();
        vocabularyRepository.deleteAll();

        apple = vocabularyRepository.save(new Vocabulary("사과", "apple", VocabularyCategory.NATIVE_KOREAN, null));
        banana = vocabularyRepository.save(new Vocabulary("바나나", "banana", VocabularyCategory.NATIVE_KOREAN, null));
        grape = vocabularyRepository.save(new Vocabulary("포도", "grape", VocabularyCategory.NATIVE_KOREAN, null));
        vocabularyRepository.save(new Vocabulary("가교", "bridge", VocabularyCategory.SINO_KOREAN, null));
    }

    @Test
    void completeQuizSavesHistoryFromServerSubmissionResults() throws Exception {
        TestSession correctSession = saveSession(apple, apple, QuizMode.WORD_TO_MEANING);
        TestSession wrongSession = saveSession(banana, grape, QuizMode.WORD_TO_MEANING);
        submit(correctSession);
        submit(wrongSession);

        mockMvc.perform(post("/api/statistics/quiz-completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NATIVE_KOREAN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionIds": ["%s", "%s"],
                                  "wrongAnswerReview": false
                                }
                                """.formatted(correctSession.questionId(), wrongSession.questionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.correctCount").value(1))
                .andExpect(jsonPath("$.incorrectCount").value(1))
                .andExpect(jsonPath("$.accuracy").value(50));

        mockMvc.perform(get("/api/statistics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.totalQuestionCount").value(2))
                .andExpect(jsonPath("$.total.correctCount").value(1))
                .andExpect(jsonPath("$.total.incorrectCount").value(1))
                .andExpect(jsonPath("$.total.accuracy").value(50))
                .andExpect(jsonPath("$.total.completedQuizCount").value(1))
                .andExpect(jsonPath("$.today.totalQuestionCount").value(2))
                .andExpect(jsonPath("$.categories[0].category").value("NATIVE_KOREAN"))
                .andExpect(jsonPath("$.categories[0].totalQuestionCount").value(2))
                .andExpect(jsonPath("$.modes[0].mode").value("WORD_TO_MEANING"))
                .andExpect(jsonPath("$.modes[0].totalQuestionCount").value(2))
                .andExpect(jsonPath("$.recentHistories", hasSize(1)));
    }

    @Test
    void dashboardReturnsEmptyStatsWhenThereIsNoHistory() throws Exception {
        mockMvc.perform(get("/api/statistics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.totalQuestionCount").value(0))
                .andExpect(jsonPath("$.total.accuracy").value(0))
                .andExpect(jsonPath("$.today.totalQuestionCount").value(0))
                .andExpect(jsonPath("$.categories", hasSize(6)))
                .andExpect(jsonPath("$.modes", hasSize(3)))
                .andExpect(jsonPath("$.recentHistories", hasSize(0)))
                .andExpect(jsonPath("$.mostWrongVocabularies", hasSize(0)));
    }

    @Test
    void dashboardReturnsMostWrongVocabularies() throws Exception {
        WrongAnswer appleWrongAnswer = wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));
        appleWrongAnswer.increaseWrongCount();
        appleWrongAnswer.increaseWrongCount();
        wrongAnswerRepository.save(appleWrongAnswer);
        wrongAnswerRepository.save(new WrongAnswer(banana, QuizMode.WORD_TO_MEANING));

        mockMvc.perform(get("/api/statistics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mostWrongVocabularies", hasSize(2)))
                .andExpect(jsonPath("$.mostWrongVocabularies[0].word").value("사과"))
                .andExpect(jsonPath("$.mostWrongVocabularies[0].wrongCount").value(3));
    }

    @Test
    void rejectCompletionBeforeAllQuestionsAreSubmitted() throws Exception {
        TestSession session = saveSession(apple, apple, QuizMode.WORD_TO_MEANING);

        mockMvc.perform(post("/api/statistics/quiz-completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NATIVE_KOREAN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionIds": ["%s"],
                                  "wrongAnswerReview": false
                                }
                                """.formatted(session.questionId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("All quiz questions must be submitted before completion."));
    }

    @Test
    void rejectWrongAnswerReviewCompletion() throws Exception {
        TestSession session = saveSession(apple, apple, QuizMode.WORD_TO_MEANING);
        submit(session);

        mockMvc.perform(post("/api/statistics/quiz-completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NATIVE_KOREAN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionIds": ["%s"],
                                  "wrongAnswerReview": true
                                }
                                """.formatted(session.questionId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Wrong answer review quizzes are not included in general learning statistics."));
    }

    private void submit(TestSession session) throws Exception {
        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(session.questionId(), session.selectedOptionId())))
                .andExpect(status().isOk());
    }

    private TestSession saveSession(Vocabulary correctVocabulary, Vocabulary selectedVocabulary, QuizMode mode) {
        String questionId = UUID.randomUUID().toString();
        String correctOptionId = UUID.randomUUID().toString();
        String selectedOptionId = correctVocabulary.getId().equals(selectedVocabulary.getId())
                ? correctOptionId
                : UUID.randomUUID().toString();

        Map<String, Long> optionVocabularyIds = new HashMap<>();
        optionVocabularyIds.put(correctOptionId, correctVocabulary.getId());
        optionVocabularyIds.put(selectedOptionId, selectedVocabulary.getId());

        sessionStore.save(new QuizQuestionSession(
                questionId,
                correctVocabulary.getId(),
                mode,
                Map.copyOf(optionVocabularyIds),
                correctOptionId,
                mode == QuizMode.WORD_TO_MEANING ? correctVocabulary.getMeaning() : correctVocabulary.getWord(),
                sessionStore.expiresAt()
        ));

        return new TestSession(questionId, selectedOptionId);
    }

    private record TestSession(String questionId, String selectedOptionId) {
    }
}
