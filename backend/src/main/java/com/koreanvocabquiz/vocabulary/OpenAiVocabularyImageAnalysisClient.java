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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiVocabularyImageAnalysisClient implements VocabularyImageAnalysisClient {

    private static final String RESPONSES_API_URL = "https://api.openai.com/v1/responses";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiVocabularyImageAnalysisClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.vision-model:gpt-4.1-mini}") String model
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<VocabularyImageAnalysisResult> extract(List<VocabularyImageFile> images) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VocabularyImageExtractionException("OPENAI_API_KEY is not configured.");
        }

        try {
            String response = restClient.post()
                    .uri(RESPONSES_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(createRequestBody(images))
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (RestClientException exception) {
            throw new VocabularyImageExtractionException("AI API call failed.", exception);
        }
    }

    private Map<String, Object> createRequestBody(List<VocabularyImageFile> images) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "input_text",
                "text", """
                        Extract only target vocabulary entries from these Korean study material images.
                        Return word, meaning, category, needsReview, confidence, and imageNumber.
                        Exclude problem numbers, page numbers, unit titles, examples, explanatory sentences, headers, and non-target descriptions.
                        Categories must be one of NATIVE_KOREAN, SINO_KOREAN, LOANWORD, PROVERB, IDIOM.
                        If classification is uncertain, choose the closest category, set needsReview true, and lower confidence.
                        """
        ));

        for (VocabularyImageFile image : images) {
            content.add(Map.of(
                    "type", "input_text",
                    "text", "The next image is imageNumber " + image.imageNumber() + "."
            ));
            content.add(Map.of(
                    "type", "input_image",
                    "image_url", "data:" + image.mediaType() + ";base64," + Base64.getEncoder().encodeToString(image.content())
            ));
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("items"));
        schema.put("properties", Map.of(
                "items", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "additionalProperties", false,
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
                "model", model,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", content
                )),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "vocabulary_image_extraction",
                                "strict", true,
                                "schema", schema
                        )
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
        JsonNode outputText = root.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return outputText.asText();
        }

        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.get("content");
                if (content == null || !content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.get("text");
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
