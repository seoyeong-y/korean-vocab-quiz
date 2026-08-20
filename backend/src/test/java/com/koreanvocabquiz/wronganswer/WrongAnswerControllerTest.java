package com.koreanvocabquiz.wronganswer;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.koreanvocabquiz.quiz.QuizMode;
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
        "spring.datasource.url=jdbc:h2:mem:wrong-answer-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class WrongAnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private WrongAnswerRepository wrongAnswerRepository;

    private Vocabulary apple;
    private Vocabulary banana;
    private Vocabulary grape;
    private Vocabulary strawberry;

    @BeforeEach
    void setUp() {
        wrongAnswerRepository.deleteAll();
        vocabularyRepository.deleteAll();

        apple = vocabularyRepository.save(new Vocabulary("사과", "apple", VocabularyCategory.NOUN, null));
        banana = vocabularyRepository.save(new Vocabulary("바나나", "banana", VocabularyCategory.NOUN, null));
        grape = vocabularyRepository.save(new Vocabulary("포도", "grape", VocabularyCategory.NOUN, null));
        strawberry = vocabularyRepository.save(new Vocabulary("딸기", "strawberry", VocabularyCategory.NOUN, null));
    }

    @Test
    void saveWrongAnswerWhenGeneralQuizSubmissionIsWrong() throws Exception {
        submit(apple, banana, QuizMode.WORD_TO_MEANING, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));

        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vocabularyId").value(apple.getId()))
                .andExpect(jsonPath("$[0].word").value("사과"))
                .andExpect(jsonPath("$[0].meaning").value("apple"))
                .andExpect(jsonPath("$[0].category").value("NOUN"))
                .andExpect(jsonPath("$[0].quizMode").value("WORD_TO_MEANING"))
                .andExpect(jsonPath("$[0].wrongCount").value(1))
                .andExpect(jsonPath("$[0].lastWrongAt").exists());
    }

    @Test
    void increaseWrongCountWhenSameVocabularyAndModeIsWrongAgain() throws Exception {
        submit(apple, banana, QuizMode.WORD_TO_MEANING, false).andExpect(status().isOk());
        submit(apple, grape, QuizMode.WORD_TO_MEANING, false).andExpect(status().isOk());

        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].wrongCount").value(2));
    }

    @Test
    void keepExistingWrongAnswerWhenGeneralQuizSubmissionIsCorrect() throws Exception {
        submit(apple, banana, QuizMode.WORD_TO_MEANING, false).andExpect(status().isOk());
        submit(apple, apple, QuizMode.WORD_TO_MEANING, false).andExpect(status().isOk());

        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].wrongCount").value(1));
    }

    @Test
    void deleteWrongAnswer() throws Exception {
        WrongAnswer wrongAnswer = wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));

        mockMvc.perform(delete("/api/wrong-answers/{id}", wrongAnswer.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteAllWrongAnswers() throws Exception {
        wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));
        wrongAnswerRepository.save(new WrongAnswer(banana, QuizMode.WORD_TO_MEANING));

        mockMvc.perform(delete("/api/wrong-answers"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createWrongAnswerReviewQuiz() throws Exception {
        saveFourWrongAnswers();

        mockMvc.perform(post("/api/wrong-answers/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "WORD_TO_MEANING",
                                  "questionCount": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].mode").value("WORD_TO_MEANING"))
                .andExpect(jsonPath("$[0].options", hasSize(4)))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
    }

    @Test
    void removeWrongAnswerWhenReviewSubmissionIsCorrect() throws Exception {
        wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));

        submit(apple, apple, QuizMode.WORD_TO_MEANING, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void increaseWrongCountWhenReviewSubmissionIsWrong() throws Exception {
        wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));

        submit(apple, banana, QuizMode.WORD_TO_MEANING, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));

        mockMvc.perform(get("/api/wrong-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].wrongCount").value(2));
    }

    @Test
    void rejectWrongAnswerReviewQuizWhenThereAreNoWrongAnswers() throws Exception {
        mockMvc.perform(post("/api/wrong-answers/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "WORD_TO_MEANING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("No wrong answers are available for review."));
    }

    @Test
    void rejectWrongAnswerReviewQuizWhenThereAreLessThanFourWrongAnswers() throws Exception {
        wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));
        wrongAnswerRepository.save(new WrongAnswer(banana, QuizMode.WORD_TO_MEANING));
        wrongAnswerRepository.save(new WrongAnswer(grape, QuizMode.WORD_TO_MEANING));

        mockMvc.perform(post("/api/wrong-answers/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "WORD_TO_MEANING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("At least 4 vocabularies are required in the category to create multiple-choice quizzes."));
    }

    private org.springframework.test.web.servlet.ResultActions submit(
            Vocabulary correctVocabulary,
            Vocabulary selectedVocabulary,
            QuizMode mode,
            boolean wrongAnswerReview
    ) throws Exception {
        return mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "vocabularyId": %d,
                          "mode": "%s",
                          "selectedOptionId": %d,
                          "wrongAnswerReview": %s
                        }
                        """.formatted(correctVocabulary.getId(), mode.name(), selectedVocabulary.getId(), wrongAnswerReview)));
    }

    private void saveFourWrongAnswers() {
        wrongAnswerRepository.save(new WrongAnswer(apple, QuizMode.WORD_TO_MEANING));
        wrongAnswerRepository.save(new WrongAnswer(banana, QuizMode.WORD_TO_MEANING));
        wrongAnswerRepository.save(new WrongAnswer(grape, QuizMode.WORD_TO_MEANING));
        wrongAnswerRepository.save(new WrongAnswer(strawberry, QuizMode.WORD_TO_MEANING));
    }
}
