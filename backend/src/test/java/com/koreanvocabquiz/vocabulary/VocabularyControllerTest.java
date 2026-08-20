package com.koreanvocabquiz.vocabulary;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vocabulary-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class VocabularyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @BeforeEach
    void setUp() {
        vocabularyRepository.deleteAll();
    }

    @Test
    void createVocabulary() throws Exception {
        mockMvc.perform(post("/api/vocabularies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "가교",
                                  "meaning": "둘 사이를 이어 주는 것",
                                  "exampleSentence": "그는 양국 협력의 가교 역할을 했다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/vocabularies/\\d+")))
                .andExpect(jsonPath("$.word").value("가교"))
                .andExpect(jsonPath("$.meaning").value("둘 사이를 이어 주는 것"))
                .andExpect(jsonPath("$.category").value("GENERAL"))
                .andExpect(jsonPath("$.exampleSentence").value("그는 양국 협력의 가교 역할을 했다."));
    }

    @Test
    void findAllVocabularies() throws Exception {
        vocabularyRepository.save(new Vocabulary("각별하다", "관계나 태도가 보통과 다르다", null));
        vocabularyRepository.save(new Vocabulary("간과하다", "대충 보아 넘기다", VocabularyCategory.VERB, "중요한 단서를 간과했다."));

        mockMvc.perform(get("/api/vocabularies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].word").value("각별하다"))
                .andExpect(jsonPath("$[0].category").value("GENERAL"))
                .andExpect(jsonPath("$[1].word").value("간과하다"))
                .andExpect(jsonPath("$[1].category").value("VERB"));
    }

    @Test
    void findVocabularyById() throws Exception {
        Vocabulary vocabulary = vocabularyRepository.save(new Vocabulary("감쇄", "힘이나 세력이 줄어 약해짐", null));

        mockMvc.perform(get("/api/vocabularies/{id}", vocabulary.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("감쇄"))
                .andExpect(jsonPath("$.meaning").value("힘이나 세력이 줄어 약해짐"));
    }

    @Test
    void updateVocabulary() throws Exception {
        Vocabulary vocabulary = vocabularyRepository.save(new Vocabulary("고양", "높이 쳐들어 올림", null));

        mockMvc.perform(put("/api/vocabularies/{id}", vocabulary.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "고양하다",
                                  "meaning": "정신이나 사기를 북돋워 높이다",
                                  "category": "VERB",
                                  "exampleSentence": "선수들의 사기를 고양했다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.word").value("고양하다"))
                .andExpect(jsonPath("$.meaning").value("정신이나 사기를 북돋워 높이다"))
                .andExpect(jsonPath("$.category").value("VERB"))
                .andExpect(jsonPath("$.exampleSentence").value("선수들의 사기를 고양했다."));
    }

    @Test
    void preserveCategoryWhenUpdateRequestDoesNotIncludeCategory() throws Exception {
        Vocabulary vocabulary = vocabularyRepository.save(new Vocabulary("간과하다", "대충 보아 넘기다", VocabularyCategory.VERB, null));

        mockMvc.perform(put("/api/vocabularies/{id}", vocabulary.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "간과하다",
                                  "meaning": "중요한 것을 지나쳐 보다"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("VERB"));
    }

    @Test
    void deleteVocabulary() throws Exception {
        Vocabulary vocabulary = vocabularyRepository.save(new Vocabulary("기민하다", "눈치가 빠르고 동작이 날쌔다", null));

        mockMvc.perform(delete("/api/vocabularies/{id}", vocabulary.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vocabularies/{id}", vocabulary.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/vocabularies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "",
                                  "meaning": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void uploadVocabularyCsv() throws Exception {
        vocabularyRepository.save(new Vocabulary("기존", "이미 있는 뜻", VocabularyCategory.GENERAL, null));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vocabularies.csv",
                "text/csv",
                """
                        word,meaning,category
                        기존,이미 있는 뜻,GENERAL
                        신규,새 뜻,NOUN
                        ,뜻만 있음,VERB
                        잘못된카테고리,뜻,UNKNOWN
                        신규,새 뜻,NOUN
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/vocabularies/csv").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.skippedRows", hasSize(2)))
                .andExpect(jsonPath("$.skippedRows[0].rowNumber").value(2))
                .andExpect(jsonPath("$.skippedRows[0].reason").value("Already exists with the same word, meaning, and category."))
                .andExpect(jsonPath("$.skippedRows[1].rowNumber").value(6))
                .andExpect(jsonPath("$.skippedRows[1].reason").value("Duplicate row in the same CSV upload."))
                .andExpect(jsonPath("$.failedRows", hasSize(2)))
                .andExpect(jsonPath("$.failedRows[0].rowNumber").value(4))
                .andExpect(jsonPath("$.failedRows[0].reason").value("word is required"))
                .andExpect(jsonPath("$.failedRows[1].rowNumber").value(5))
                .andExpect(jsonPath("$.failedRows[1].reason").value(matchesPattern("category must be one of .*")));

        mockMvc.perform(get("/api/vocabularies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void rejectCsvWithoutRequiredHeaders() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vocabularies.csv",
                "text/csv",
                """
                        word,meaning
                        단어,뜻
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/vocabularies/csv").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("CSV must contain word, meaning, and category columns."));
    }
}
