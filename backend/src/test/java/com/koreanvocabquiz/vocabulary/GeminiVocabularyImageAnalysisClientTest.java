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
        Map<String, Object> requestBody = client.createRequestBody(
                new VocabularyImageFile(1, "image/jpeg", "image".getBytes(StandardCharsets.UTF_8))
        );

        List<Map<String, Object>> contents = (List<Map<String, Object>>) requestBody.get("contents");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String prompt = (String) parts.get(0).get("text");
        long imagePartCount = parts.stream()
                .filter(part -> part.containsKey("inlineData"))
                .count();

        assertThat(prompt).contains(
                "You are a strict OCR transcription tool for Korean vocabulary images.",
                "This request contains exactly one image.",
                "Return items only from this one image.",
                "Do not use text from any other image or previous request.",
                "Every returned item must use the imageNumber provided immediately before the image.",
                "Read only text that is visibly present in the image pixels.",
                "Return an item only when both a word and its meaning are explicitly visible in the image.",
                "Copy word exactly as visible. Copy meaning exactly as visible.",
                "If one visible word has multiple meanings marked with number symbols such as ① ② ③ ④ ⑤, return one item for that word.",
                "Keep those numbered meanings together in a single meaning string, preserving the visible number symbols.",
                "Do not split numbered meanings for the same word into separate items.",
                "Do not add words that are not visible in the image.",
                "Do not use dictionary knowledge, Korean language knowledge, context, textbook patterns, or common sense to fill any value.",
                "If either word or meaning is unclear, blurred, cut off, partially hidden, or not explicitly paired, omit the item.",
                "It is better to return fewer items or an empty items array than to create content not visible in the image."
        );
        assertThat(prompt).doesNotContain("Extract target vocabulary entries");
        assertThat(parts.get(1).get("text")).isEqualTo("The only image in this request is imageNumber 1.");
        assertThat(imagePartCount).isEqualTo(1);
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
