package com.koreanvocabquiz.vocabulary;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeminiVocabularyImageAnalysisClientTest {

    private final GeminiVocabularyImageAnalysisClient client = new GeminiVocabularyImageAnalysisClient(
            RestClient.builder(),
            new ObjectMapper(),
            "test-key",
            "gemini-2.5-flash"
    );

    @Test
    @SuppressWarnings("unchecked")
    void createRequestBodyUsesStrictVisibleTextPrompt() {
        Map<String, Object> requestBody = client.createRequestBody(List.of(
                new VocabularyImageFile(1, "image/jpeg", "image".getBytes(StandardCharsets.UTF_8))
        ));

        List<Map<String, Object>> contents = (List<Map<String, Object>>) requestBody.get("contents");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String prompt = (String) parts.get(0).get("text");

        assertThat(prompt).contains(
                "Read only the text visibly present in the image.",
                "Do not add, infer, complete, correct, paraphrase, or supplement any word or meaning using outside knowledge.",
                "Do not replace or improve meanings with dictionary definitions.",
                "If text is unclear, blurred, cut off, or partially hidden, do not guess.",
                "Omit uncertain content or mark it as needsReview.",
                "Missing uncertain content is better than creating incorrect content."
        );
    }

    @Test
    void parseStructuredOutputResponse() {
        List<VocabularyImageAnalysisResult> results = client.parseResponse("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"items\\":[{\\"imageNumber\\":1,\\"word\\":\\"가람\\",\\"meaning\\":\\"강\\",\\"category\\":\\"NATIVE_KOREAN\\",\\"needsReview\\":false,\\"confidence\\":0.92}]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).imageNumber()).isEqualTo(1);
        assertThat(results.get(0).word()).isEqualTo("가람");
        assertThat(results.get(0).meaning()).isEqualTo("강");
        assertThat(results.get(0).category()).isEqualTo("NATIVE_KOREAN");
        assertThat(results.get(0).needsReview()).isFalse();
        assertThat(results.get(0).confidence()).isEqualTo(0.92);
    }

    @Test
    void rejectInvalidStructuredOutputResponse() {
        assertThatThrownBy(() -> client.parseResponse("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"invalid\\":[]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """))
                .isInstanceOf(VocabularyImageExtractionException.class)
                .hasMessage("AI response format is invalid.");
    }
}
