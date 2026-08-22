package com.koreanvocabquiz.literature;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import com.koreanvocabquiz.common.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:literature-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.admin-password="
})
@AutoConfigureMockMvc
class LiteratureControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private LiteraryFeatureRepository featureRepository;
    @Autowired private LiteraryWorkRepository workRepository;
    @Autowired private LiteraryAuthorRepository authorRepository;
    @MockBean private LiteraryImageAnalysisClient imageAnalysisClient;

    @BeforeEach
    void setUp() {
        featureRepository.deleteAll();
        workRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    void authorWorkAndFeatureCrudUsesRelationships() throws Exception {
        MvcResult authorResult = mockMvc.perform(post("/api/literature/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"현진건\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("현진건"))
                .andReturn();
        Long authorId = authorRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/literature/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":" + authorId + ",\"title\":\"운수 좋은 날\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("현진건"));
        Long workId = workRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/literature/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":" + authorId + ",\"workId\":" + workId + ",\"type\":\"WORK\",\"content\":\"작품 고유 특징\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WORK"))
                .andExpect(jsonPath("$.workTitle").value("운수 좋은 날"));
    }

    @Test
    void workFeatureRequiresWorkAndAuthorFeatureDoesNotAcceptWork() throws Exception {
        Long authorId = authorRepository.save(new LiteraryAuthor("김유정")).getId();
        mockMvc.perform(post("/api/literature/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":" + authorId + ",\"type\":\"WORK\",\"content\":\"작품 특징\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csvPreviewInheritsAuthorButDoesNotForwardFillFeature() throws Exception {
        MockMultipartFile csv = new MockMultipartFile("file", "literature.csv", "text/csv",
                ("\uFEFFauthor,work,feature,feature_type\n" +
                        "김유정,동백꽃,,\n" +
                        ",소낙비,,\n" +
                        ",,농촌의 현실을 해학적으로 그림,AUTHOR\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/literature/csv/preview").file(csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.rows[1].author").value("김유정"))
                .andExpect(jsonPath("$.rows[1].feature").doesNotExist())
                .andExpect(jsonPath("$.rows[2].author").value("김유정"))
                .andExpect(jsonPath("$.rows[2].status").value("NORMAL"));
        org.junit.jupiter.api.Assertions.assertEquals(0, authorRepository.count());
    }

    @Test
    void literaryQuizUsesFourUniqueOptionsAndValidatesQuestionOption() throws Exception {
        for (String authorName : new String[]{"현진건", "김유정", "이상", "이효석"}) {
            LiteraryAuthor author = authorRepository.save(new LiteraryAuthor(authorName));
            LiteraryWork work = workRepository.save(new LiteraryWork(author, authorName + " 작품"));
            featureRepository.save(new LiteraryFeature(author, work, LiteratureFeatureType.WORK, authorName + " 작품 특징"));
        }

        MvcResult result = mockMvc.perform(post("/api/literature/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizType\":\"WORK_GUESS\",\"questionCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].options.length()").value(4))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get(0);
        String questionId = json.get("questionId").asText();
        String optionId = json.get("options").get(0).get("optionId").asText();

        mockMvc.perform(post("/api/literature/quizzes/{questionId}/submit", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionId\":\"not-an-option\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/literature/quizzes/{questionId}/submit", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionId\":\"" + optionId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").exists());
    }

    @Test
    void authorFeatureCreatesWorkGuessAndAuthorGuessWithoutAmbiguousOptions() throws Exception {
        LiteraryAuthor target = authorRepository.save(new LiteraryAuthor("김유정"));
        workRepository.save(new LiteraryWork(target, "동백꽃"));
        workRepository.save(new LiteraryWork(target, "봄봄"));
        featureRepository.save(new LiteraryFeature(target, null, LiteratureFeatureType.AUTHOR, "농촌을 해학적으로 그린 작가"));
        for (String name : new String[]{"현진건", "이상", "이효석"}) {
            LiteraryAuthor author = authorRepository.save(new LiteraryAuthor(name));
            workRepository.save(new LiteraryWork(author, name + " 작품"));
        }

        mockMvc.perform(post("/api/literature/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizType\":\"WORK_GUESS\",\"questionCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionText").value("다음 중 이 작가의 작품은?"))
                .andExpect(jsonPath("$[0].options.length()").value(4));

        mockMvc.perform(post("/api/literature/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizType\":\"AUTHOR_GUESS\",\"questionCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionText").value("이 작가는?"))
                .andExpect(jsonPath("$[0].workTitles.length()").value(2))
                .andExpect(jsonPath("$[0].options.length()").value(4));
    }

    @Test
    void csvImportCreatesRowsAndSkipsExactDuplicates() throws Exception {
        String payload = "{\"rows\":["
                + "{\"rowNumber\":2,\"selected\":true,\"author\":\"김유정\",\"work\":\"동백꽃\",\"feature\":null,\"featureType\":null},"
                + "{\"rowNumber\":3,\"selected\":true,\"author\":\"김유정\",\"work\":\"동백꽃\",\"feature\":null,\"featureType\":null},"
                + "{\"rowNumber\":4,\"selected\":true,\"author\":\"김유정\",\"work\":null,\"feature\":\"농촌 특징\",\"featureType\":\"AUTHOR\"}]}";
        mockMvc.perform(post("/api/literature/csv/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void unresolvedFeatureCannotBeImportedUntilTypeIsConfirmed() throws Exception {
        mockMvc.perform(post("/api/literature/image/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rows\":[{\"rowNumber\":1,\"selected\":true,\"author\":\"현진건\",\"work\":null,\"feature\":\"확인 필요한 설명\",\"featureType\":null}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(1));
    }

    @Test
    void literaryImageExtractionNormalizesAuthorAndWorkWithoutSaving() throws Exception {
        when(imageAnalysisClient.extract(any())).thenReturn(List.of(new LiteraryImageAnalysisResult(1, List.of(
                new LiteraryImageAuthorDraft("현진건(소설)", List.of("<운수 좋은 날>", "〈빈처〉"), List.of(
                        new LiteraryImageFeatureDraft("WORK", "운수 좋은 날", "작품 특징", false),
                        new LiteraryImageFeatureDraft("UNRESOLVED", null, "작가 설명", true)
                ))
        ))));
        MockMultipartFile image = new MockMultipartFile("files", "page.jpg", "image/jpeg", "image".getBytes());

        mockMvc.perform(multipart("/api/literature/image/extract").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(4))
                .andExpect(jsonPath("$.rows[0].author").value("현진건"))
                .andExpect(jsonPath("$.rows[0].work").value("운수 좋은 날"))
                .andExpect(jsonPath("$.rows[2].featureType").value("WORK"))
                .andExpect(jsonPath("$.rows[3].featureType").value("UNRESOLVED"));
        org.junit.jupiter.api.Assertions.assertEquals(0, authorRepository.count());
    }
}
