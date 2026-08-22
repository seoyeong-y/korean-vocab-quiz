package com.koreanvocabquiz.literature;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:literature-auth-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.admin-password=literature-admin"
})
@AutoConfigureMockMvc
class LiteratureAdminAuthenticationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void literatureMutationRequiresAdminAuthentication() throws Exception {
        mockMvc.perform(post("/api/literature/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김유정\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void literatureImageExtractionRequiresAdminAuthentication() throws Exception {
        MockMultipartFile image = new MockMultipartFile("files", "page.jpg", "image/jpeg", "not-an-image".getBytes());
        mockMvc.perform(multipart("/api/literature/image/extract").file(image))
                .andExpect(status().isUnauthorized());
    }
}
