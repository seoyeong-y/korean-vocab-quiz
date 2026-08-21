package com.koreanvocabquiz.vocabulary;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GeminiVocabularyImageAnalysisClient implements VocabularyImageAnalysisClient {

    private static final String GENERATE_CONTENT_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiVocabularyImageAnalysisClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.vision-model:gemini-2.5-flash}") String model
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<VocabularyImageAnalysisResult> extract(List<VocabularyImageFile> images) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VocabularyImageExtractionException("GEMINI_API_KEY is not configured.");
        }

        try {
            String response = restClient.post()
                    .uri(createRequestUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequestBody(images))
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (RestClientException exception) {
            throw new VocabularyImageExtractionException("AI API call failed.", exception);
        }
    }

    private String createRequestUri() {
        return UriComponentsBuilder
                .fromUriString(GENERATE_CONTENT_API_URL + model + ":generateContent")
                .queryParam("key", apiKey)
                .build()
                .toUriString();
    }

    private Map<String, Object> createRequestBody(List<VocabularyImageFile> images) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of(
                "text", """
                        Extract only target vocabulary entries from these Korean study material images.
                        Return word, meaning, category, needsReview, confidence, and imageNumber.
                        Exclude problem numbers, page numbers, unit titles, examples, explanatory sentences, headers, and non-target descriptions.
                        Categories must be one of NATIVE_KOREAN, SINO_KOREAN, LOANWORD, PROVERB, IDIOM.
                        If classification is uncertain, choose the closest category, set needsReview true, and lower confidence.
                        """
        ));

        for (VocabularyImageFile image : images) {
            parts.add(Map.of(
                    "text", "The next image is imageNumber " + image.imageNumber() + "."
            ));
            parts.add(Map.of(
                    "inlineData", Map.of(
                            "mimeType", image.mediaType(),
                            "data", Base64.getEncoder().encodeToString(image.content())
                    )
            ));
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("items"));
        schema.put("properties", Map.of(
                "items", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "required", List.of("imageNumber", "word", "meaning", "category", "needsReview", "confidence"),
                                "properties", Map.of(
                                        "imageNumber", Map.of("type", "integer"),
                                        "word", Map.of("type", "string"),
                                        "meaning", Map.of("type", "string"),
                                        "category", Map.of(
                                                "type", "string",
                                                "enum", List.of("NATIVE_KOREAN", "SINO_KOREAN", "LOANWORD", "PROVERB", "IDIOM")
                                        ),
                                        "needsReview", Map.of("type", "boolean"),
                                        "confidence", Map.of("type", "number")
                                )
                        )
                )
        ));

        return Map.of(
                "contents", List.of(Map.of(
                        "parts", parts
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", schema
                )
        );
    }

    List<VocabularyImageAnalysisResult> parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String outputText = findOutputText(root);
            if (outputText == null || outputText.isBlank()) {
                throw new VocabularyImageExtractionException("AI response format is invalid.");
            }

            return parseOutputText(outputText);
        } catch (JsonProcessingException exception) {
            throw new VocabularyImageExtractionException("AI response format is invalid.", exception);
        }
    }

    private String findOutputText(JsonNode root) {
        JsonNode candidates = root.get("candidates");
        if (candidates != null && candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").get("parts");
                if (parts == null || !parts.isArray()) {
                    continue;
                }
                for (JsonNode part : parts) {
                    JsonNode text = part.get("text");
                    if (text != null && text.isTextual()) {
                        return text.asText();
                    }
                }
            }
        }

        return null;
    }

    private List<VocabularyImageAnalysisResult> parseOutputText(String outputText) {
        try {
            String normalized = stripJsonFence(outputText);
            JsonNode root = objectMapper.readTree(normalized);
            JsonNode items = root.isArray() ? root : root.get("items");

            if (items == null || !items.isArray()) {
                throw new VocabularyImageExtractionException("AI response format is invalid.");
            }

            List<VocabularyImageAnalysisResult> results = new ArrayList<>();
            for (JsonNode item : items) {
                results.add(new VocabularyImageAnalysisResult(
                        readInt(item, "imageNumber"),
                        readText(item, "word"),
                        readText(item, "meaning"),
                        readText(item, "category"),
                        readBoolean(item, "needsReview"),
                        item.hasNonNull("confidence") && item.get("confidence").isNumber()
                                ? item.get("confidence").asDouble()
                                : null
                ));
            }
            return results;
        } catch (JsonProcessingException exception) {
            throw new VocabularyImageExtractionException("AI response format is invalid.", exception);
        }
    }

    private String stripJsonFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private String readText(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value != null && value.isTextual() ? value.asText().trim() : "";
    }

    private int readInt(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value != null && value.canConvertToInt() ? value.asInt() : 0;
    }

    private boolean readBoolean(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value != null && value.isBoolean() && value.asBoolean();
    }
}
