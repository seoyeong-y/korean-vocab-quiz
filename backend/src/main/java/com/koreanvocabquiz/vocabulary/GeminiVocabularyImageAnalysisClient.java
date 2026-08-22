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

        List<VocabularyImageAnalysisResult> results = new ArrayList<>();
        for (VocabularyImageFile image : images) {
            results.addAll(extractSingleImage(image));
        }
        return results;
    }

    private List<VocabularyImageAnalysisResult> extractSingleImage(VocabularyImageFile image) {
        try {
            String response = restClient.post()
                    .uri(createRequestUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequestBody(image))
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

    Map<String, Object> createRequestBody(VocabularyImageFile image) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of(
                "text", """
                        You are a strict OCR transcription tool for Korean vocabulary images.
                        Your job is NOT to solve, explain, normalize, improve, or complete the material.

                        This request contains exactly one image.
                        Return items only from this one image.
                        Do not use text from any other image or previous request.
                        Every returned item must use the imageNumber provided immediately before the image.
                        Read only text that is visibly present in the image pixels.
                        Return an item only when both a word and its meaning are explicitly visible in the image.
                        Copy word exactly as visible. Copy meaning exactly as visible.
                        For loanword notation questions where two Korean spellings are visible and one is marked as the correct spelling, use the correct Korean spelling as word.
                        In that loanword case, use only the visibly shown original foreign word, such as English, as meaning.
                        If one visible word has multiple meanings marked with number symbols such as ① ② ③ ④ ⑤, return one item for that word.
                        Keep those numbered meanings together in a single meaning string, preserving the visible number symbols.
                        Do not split numbered meanings for the same word into separate items.
                        Do not add words that are not visible in the image.
                        Do not add meanings that are not visible in the image.
                        Do not infer missing letters, complete cropped text, correct spelling, paraphrase, summarize, translate, or supplement content.
                        Do not use dictionary knowledge, Korean language knowledge, context, textbook patterns, or common sense to fill any value.
                        If either word or meaning is unclear, blurred, cut off, partially hidden, or not explicitly paired, omit the item.
                        If a visible item is readable but category is uncertain, include it with needsReview true and low confidence.
                        It is better to return fewer items or an empty items array than to create content not visible in the image.

                        Return only word, meaning, category, needsReview, confidence, and imageNumber.
                        Categories must be one of NATIVE_KOREAN, SINO_KOREAN, LOANWORD, PROVERB, IDIOM, FOUR_CHARACTER_IDIOM.
                        Category may be classified by AI only after word and meaning have been copied from visible text.
                        """
        ));

        parts.add(Map.of(
                "text", "The only image in this request is imageNumber " + image.imageNumber() + "."
        ));
        parts.add(Map.of(
                "inlineData", Map.of(
                        "mimeType", image.mediaType(),
                        "data", Base64.getEncoder().encodeToString(image.content())
                )
        ));

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
                                                "enum", List.of("NATIVE_KOREAN", "SINO_KOREAN", "LOANWORD", "PROVERB", "IDIOM", "FOUR_CHARACTER_IDIOM")
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
