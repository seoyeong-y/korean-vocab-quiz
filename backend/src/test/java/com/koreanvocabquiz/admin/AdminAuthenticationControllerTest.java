package com.koreanvocabquiz.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.koreanvocabquiz.vocabulary.VocabularyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.admin-password=test-admin-password"
})
@AutoConfigureMockMvc
class AdminAuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @BeforeEach
    void setUp() {
        vocabularyRepository.deleteAll();
    }

    @Test
    void publicVocabularyReadRemainsAvailableWithoutAdminAuthentication() throws Exception {
        mockMvc.perform(get("/api/vocabularies"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedVocabularyWriteRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/vocabularies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "가교",
                                  "meaning": "둘 사이를 이어 주는 것"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctPasswordAllowsProtectedVocabularyWrite() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/admin/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"test-admin-password\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/vocabularies")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "가교",
                                  "meaning": "둘 사이를 이어 주는 것"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
