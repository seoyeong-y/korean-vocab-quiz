package com.koreanvocabquiz.literature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeminiLiteraryImageAnalysisClientTest {
    private final GeminiLiteraryImageAnalysisClient client = new GeminiLiteraryImageAnalysisClient(
            RestClient.builder(), new ObjectMapper(), "test-key", "gemini-2.5-flash");

    @Test
    @SuppressWarnings("unchecked")
    void promptRequiresVisibleLiteratureTextAndBracketedWorks() {
        Map<String, Object> body = client.requestBody(new LiteraryImageFile(1, "image/jpeg", "image".getBytes(StandardCharsets.UTF_8)));
        List<Map<String, Object>> contents = (List<Map<String, Object>>) body.get("contents");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String prompt = (String) parts.get(0).get("text");
        assertThat(prompt).contains(
                "Read only text visibly present in this one image.",
                "For author names such as 현진건(소설), remove the trailing parenthesized genre",
                "A work is a title visibly enclosed by < >, 〈 〉, or 《 》.",
                "Never copy an AUTHOR feature to each work",
                "type UNRESOLVED and needsReview=true");
    }

    @Test
    void parsesAuthorsWorksAndUnresolvedFeature() {
        LiteraryImageAnalysisResult result = client.parseResponse("""
                {"candidates":[{"content":{"parts":[{"text":"{\\"authors\\":[{\\"name\\":\\"현진건(소설)\\",\\"works\\":[\\"빈처\\",\\"운수 좋은 날\\"],\\"features\\":[{\\"type\\":\\"UNRESOLVED\\",\\"workTitle\\":\\"\\",\\"content\\":\\"작가 설명\\",\\"needsReview\\":true}]}]}"}]}}]}
                """, 1);
        assertThat(result.authors()).hasSize(1);
        assertThat(result.authors().get(0).name()).isEqualTo("현진건(소설)");
        assertThat(result.authors().get(0).works()).containsExactly("빈처", "운수 좋은 날");
        assertThat(result.authors().get(0).features().get(0).type()).isEqualTo("UNRESOLVED");
    }

    @Test
    void rejectsInvalidFeatureType() {
        assertThatThrownBy(() -> client.parseResponse("""
                {"candidates":[{"content":{"parts":[{"text":"{\\"authors\\":[{\\"name\\":\\"현진건\\",\\"works\\":[],\\"features\\":[{\\"type\\":\\"OTHER\\",\\"workTitle\\":\\"\\",\\"content\\":\\"설명\\",\\"needsReview\\":false}]}]}"}]}}]}
                """, 1)).isInstanceOf(LiteraryImageAnalysisException.class)
                .hasMessage("AI response contains an invalid feature type.");
    }
}
