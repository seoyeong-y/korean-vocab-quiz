package com.koreanvocabquiz.quiz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreanvocabquiz.wronganswer.WrongAnswer;
import com.koreanvocabquiz.wronganswer.WrongAnswerRepository;
import com.koreanvocabquiz.vocabulary.Vocabulary;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;
import com.koreanvocabquiz.vocabulary.VocabularyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:quiz-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private MasteredVocabularyRepository masteredVocabularyRepository;

    @Autowired
    private WrongAnswerRepository wrongAnswerRepository;

    @Autowired
    private QuizQuestionSessionStore sessionStore;

    @Autowired
    private ObjectMapper objectMapper;

    private Vocabulary apple;
    private Vocabulary banana;

    @BeforeEach
    void setUp() {
        masteredVocabularyRepository.deleteAll();
        wrongAnswerRepository.deleteAll();
        vocabularyRepository.deleteAll();

        apple = vocabularyRepository.save(new Vocabulary("사과", "apple", VocabularyCategory.NOUN, null));
        banana = vocabularyRepository.save(new Vocabulary("바나나", "banana", VocabularyCategory.NOUN, null));
        vocabularyRepository.save(new Vocabulary("포도", "grape", VocabularyCategory.NOUN, null));
        vocabularyRepository.save(new Vocabulary("딸기", "strawberry", VocabularyCategory.NOUN, null));
        vocabularyRepository.save(new Vocabulary("달리다", "run", VocabularyCategory.VERB, null));
        vocabularyRepository.save(new Vocabulary("걷다", "walk", VocabularyCategory.VERB, null));
        vocabularyRepository.save(new Vocabulary("먹다", "eat", VocabularyCategory.VERB, null));
    }

    @Test
    void createWordToMeaningQuiz() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionCount": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].questionId").exists())
                .andExpect(jsonPath("$[0].vocabularyId").exists())
                .andExpect(jsonPath("$[0].mode").value("WORD_TO_MEANING"))
                .andExpect(jsonPath("$[0].questionText", not(matchesPattern("apple|banana|grape|strawberry"))))
                .andExpect(jsonPath("$[0].options", hasSize(4)))
                .andExpect(jsonPath("$[0].options[*].optionId", hasSize(4)))
                .andExpect(jsonPath("$[0].options[*].text", containsInAnyOrder("apple", "banana", "grape", "strawberry")))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
    }

    @Test
    void createMeaningToWordQuiz() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "MEANING_TO_WORD",
                                  "questionCount": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].mode").value("MEANING_TO_WORD"))
                .andExpect(jsonPath("$[0].questionText", matchesPattern("apple|banana|grape|strawberry")))
                .andExpect(jsonPath("$[0].options", hasSize(4)))
                .andExpect(jsonPath("$[0].options[*].text", containsInAnyOrder("사과", "바나나", "포도", "딸기")))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
    }

    @Test
    void createMixedQuizWithResolvedQuestionModes() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "MIXED",
                                  "questionCount": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].mode", containsInAnyOrder(
                        matchesPattern("WORD_TO_MEANING|MEANING_TO_WORD"),
                        matchesPattern("WORD_TO_MEANING|MEANING_TO_WORD"),
                        matchesPattern("WORD_TO_MEANING|MEANING_TO_WORD"),
                        matchesPattern("WORD_TO_MEANING|MEANING_TO_WORD")
                )))
                .andExpect(jsonPath("$[0].mode").value(not("MIXED")))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
    }

    @Test
    void createFourDifferentOptions() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionCount": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].options[*].text", containsInAnyOrder("apple", "banana", "grape", "strawberry")));
    }

    @Test
    void submitCorrectAnswer() throws Exception {
        TestSession session = saveSession(apple, apple, QuizMode.WORD_TO_MEANING);

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(session.questionId(), session.selectedOptionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.correctAnswer").value("apple"))
                .andExpect(jsonPath("$.vocabularyId").value(apple.getId()));
    }

    @Test
    void submitWrongAnswer() throws Exception {
        TestSession session = saveSession(apple, banana, QuizMode.MEANING_TO_WORD);

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(session.questionId(), session.selectedOptionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.correctAnswer").value("사과"))
                .andExpect(jsonPath("$.vocabularyId").value(apple.getId()));
    }

    @Test
    void markGeneratedQuestionAsMastered() throws Exception {
        QuizQuestionResponse question = createOneQuestion("WORD_TO_MEANING");

        mockMvc.perform(post("/api/quizzes/mastered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s"
                                }
                                """.formatted(question.questionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mastered").value(true))
                .andExpect(jsonPath("$.correctAnswer").exists())
                .andExpect(jsonPath("$.vocabularyId").value(question.vocabularyId()));

        assertEquals(1, masteredVocabularyRepository.count());
    }

    @Test
    void rejectMasteredRequestWithoutGeneratedQuestion() throws Exception {
        mockMvc.perform(post("/api/quizzes/mastered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Question is not valid or has expired."));
    }

    @Test
    void excludeMasteredVocabularyFromGeneralQuiz() throws Exception {
        vocabularyRepository.save(new Vocabulary("수박", "watermelon", VocabularyCategory.NOUN, null));
        QuizQuestionResponse question = createOneQuestion("WORD_TO_MEANING");
        mockMvc.perform(post("/api/quizzes/mastered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s"
                                }
                                """.formatted(question.questionId())))
                .andExpect(status().isOk());

        String content = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionCount": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<QuizQuestionResponse> questions = objectMapper.readValue(content, new TypeReference<>() {
        });
        assertEquals(0, questions.stream()
                .filter(nextQuestion -> nextQuestion.vocabularyId().equals(question.vocabularyId()))
                .count());
    }

    @Test
    void removeWrongAnswerWhenMarkingVocabularyAsMastered() throws Exception {
        wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));
        TestSession session = saveSession(apple, apple, QuizMode.WORD_TO_MEANING);

        mockMvc.perform(post("/api/quizzes/mastered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s"
                                }
                                """.formatted(session.questionId())))
                .andExpect(status().isOk());

        assertEquals(0, wrongAnswerRepository.count());
    }

    @Test
    void submitWordToMeaningAnswerFromGeneratedQuestion() throws Exception {
        QuizQuestionResponse question = createOneQuestion("WORD_TO_MEANING");
        String selectedOptionId = question.options().get(0).optionId();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(question.questionId(), selectedOptionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vocabularyId").value(question.vocabularyId()));
    }

    @Test
    void submitMeaningToWordAnswerFromGeneratedQuestion() throws Exception {
        QuizQuestionResponse question = createOneQuestion("MEANING_TO_WORD");
        String selectedOptionId = question.options().get(0).optionId();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(question.questionId(), selectedOptionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vocabularyId").value(question.vocabularyId()));
    }

    @Test
    void createOptionsWithoutDuplicateTextsWhenSameMeaningExists() throws Exception {
        vocabularyRepository.save(new Vocabulary("복숭아", "apple", VocabularyCategory.NOUN, null));
        vocabularyRepository.save(new Vocabulary("수박", "watermelon", VocabularyCategory.NOUN, null));

        QuizQuestionResponse question = createOneQuestion("WORD_TO_MEANING");

        assertEquals(
                question.options().size(),
                question.options().stream().map(QuizOptionResponse::text).collect(java.util.stream.Collectors.toCollection(HashSet::new)).size()
        );
    }

    @Test
    void createOptionsWithoutDuplicateTextsWhenSameWordExists() throws Exception {
        vocabularyRepository.save(new Vocabulary("사과", "apology", VocabularyCategory.NOUN, null));
        vocabularyRepository.save(new Vocabulary("수박", "watermelon", VocabularyCategory.NOUN, null));

        QuizQuestionResponse question = createOneQuestion("MEANING_TO_WORD");

        assertEquals(
                question.options().size(),
                question.options().stream().map(QuizOptionResponse::text).collect(java.util.stream.Collectors.toCollection(HashSet::new)).size()
        );
    }

    @Test
    void rejectOptionThatWasNotIncludedInGeneratedQuestion() throws Exception {
        QuizQuestionResponse question = createOneQuestion("WORD_TO_MEANING");

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(question.questionId(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Selected option is not included in the question."));
    }

    @Test
    void rejectSubmissionWithoutGeneratedQuestionId() throws Exception {
        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Question is not valid or has expired."));
    }

    @Test
    void rejectLegacySubmissionWithArbitraryVocabularyId() throws Exception {
        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vocabularyId": %d,
                                  "mode": "WORD_TO_MEANING",
                                  "selectedOptionId": %d
                                }
                                """.formatted(apple.getId(), apple.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void judgeByOptionIdWhenSameMeaningExists() throws Exception {
        vocabularyRepository.save(new Vocabulary("복숭아", "apple", VocabularyCategory.NOUN, null));
        vocabularyRepository.save(new Vocabulary("수박", "watermelon", VocabularyCategory.NOUN, null));

        TestSession session = saveSession(apple, banana, QuizMode.WORD_TO_MEANING);

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(session.questionId(), session.selectedOptionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.correctAnswer").value("apple"));
    }

    @Test
    void judgeByOptionIdWhenSameWordExists() throws Exception {
        vocabularyRepository.save(new Vocabulary("사과", "apology", VocabularyCategory.NOUN, null));
        vocabularyRepository.save(new Vocabulary("수박", "watermelon", VocabularyCategory.NOUN, null));

        TestSession session = saveSession(apple, banana, QuizMode.MEANING_TO_WORD);

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionId": "%s",
                                  "selectedOptionId": "%s"
                                }
                                """.formatted(session.questionId(), session.selectedOptionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.correctAnswer").value("사과"));
    }

    @Test
    void rejectCategoryWithLessThanFourVocabularies() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "VERB",
                                  "mode": "WORD_TO_MEANING",
                                  "questionCount": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("At least 4 vocabularies are required in the category to create multiple-choice quizzes."));
    }

    @Test
    void rejectQuestionCountGreaterThanVocabularyCount() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionCount": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("questionCount cannot be greater than the number of vocabularies available for this quiz."));
    }

    @Test
    void rejectInvalidCategory() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "UNKNOWN",
                                  "mode": "WORD_TO_MEANING",
                                  "questionCount": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Request body contains invalid or unreadable values."));
    }

    @Test
    void rejectInvalidMode() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "UNKNOWN",
                                  "questionCount": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Request body contains invalid or unreadable values."));
    }

    private QuizQuestionResponse createOneQuestion(String mode) throws Exception {
        String content = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "NOUN",
                                  "mode": "%s",
                                  "questionCount": 1
                                }
                                """.formatted(mode)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(content, new TypeReference<List<QuizQuestionResponse>>() {
        }).get(0);
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
