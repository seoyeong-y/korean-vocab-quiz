package com.koreanvocabquiz.quiz;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private Vocabulary apple;
    private Vocabulary banana;

    @BeforeEach
    void setUp() {
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
        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vocabularyId": %d,
                                  "mode": "WORD_TO_MEANING",
                                  "selectedOptionId": %d
                                }
                                """.formatted(apple.getId(), apple.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.correctAnswer").value("apple"))
                .andExpect(jsonPath("$.vocabularyId").value(apple.getId()));
    }

    @Test
    void submitWrongAnswer() throws Exception {
        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vocabularyId": %d,
                                  "mode": "MEANING_TO_WORD",
                                  "selectedOptionId": %d
                                }
                                """.formatted(apple.getId(), banana.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.correctAnswer").value("사과"))
                .andExpect(jsonPath("$.vocabularyId").value(apple.getId()));
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
                .andExpect(jsonPath("$.messages[0]").value("questionCount cannot be greater than the number of vocabularies in the category."));
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
}
