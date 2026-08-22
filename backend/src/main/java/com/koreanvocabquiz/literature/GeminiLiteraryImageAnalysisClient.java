package com.koreanvocabquiz.literature;

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
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GeminiLiteraryImageAnalysisClient implements LiteraryImageAnalysisClient {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiLiteraryImageAnalysisClient(
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
    public List<LiteraryImageAnalysisResult> extract(List<LiteraryImageFile> images) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LiteraryImageAnalysisException("GEMINI_API_KEY is not configured.");
        }
        List<LiteraryImageAnalysisResult> results = new ArrayList<>();
        for (LiteraryImageFile image : images) {
            results.add(extractOne(image));
        }
        return results;
    }

    private LiteraryImageAnalysisResult extractOne(LiteraryImageFile image) {
        try {
            String response = restClient.post()
                    .uri(UriComponentsBuilder.fromUriString(API_URL + model + ":generateContent")
                            .queryParam("key", apiKey).build().toUriString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(image))
                    .retrieve()
                    .body(String.class);
            return parseResponse(response, image.imageNumber());
        } catch (RestClientException exception) {
            throw new LiteraryImageAnalysisException("AI API call failed.", exception);
        }
    }

    Map<String, Object> requestBody(LiteraryImageFile image) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", """
                You are a strict OCR transcription and structure extraction tool for a Korean literature textbook table.
                Read only text visibly present in this one image. Do not use literary knowledge or another image.
                Do not add, infer, complete, correct, paraphrase, summarize, translate, or supplement any author, work, or feature.
                If text is unclear, cropped, or ambiguous, omit it or mark the feature needsReview=true.
                The source image is the only source of truth.

                Extract every visible author row, including multiple authors on one image.
                For author names such as 현진건(소설), remove the trailing parenthesized genre and return only 현진건.
                A work is a title visibly enclosed by < >, 〈 〉, or 《 》. Remove those brackets from title.
                Split multiple visibly bracketed titles into separate works.
                Do not turn ordinary explanatory text or a referenced title without title brackets into a work.
                A visible '<work>: explanation' belongs to that work as a WORK feature.
                Text describing the author, several works, or the author's general literary world is an AUTHOR feature.
                Never copy an AUTHOR feature to each work.
                If the feature type cannot be determined from the visible context, return type UNRESOLVED and needsReview=true.
                Preserve visible wording as closely as possible. Do not fix OCR unless the correction is visibly unambiguous.
                Ignore page titles, section headings, page numbers, examples, and general instructions that are not author rows.
                Return JSON only.
                """));
        parts.add(Map.of("text", "This is imageNumber " + image.imageNumber() + "."));
        parts.add(Map.of("inlineData", Map.of(
                "mimeType", image.mediaType(),
                "data", Base64.getEncoder().encodeToString(image.content())
        )));

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "object");
        feature.put("required", List.of("type", "workTitle", "content", "needsReview"));
        feature.put("properties", Map.of(
                "type", Map.of("type", "string", "enum", List.of("WORK", "AUTHOR", "UNRESOLVED")),
                "workTitle", Map.of("type", "string"),
                "content", Map.of("type", "string"),
                "needsReview", Map.of("type", "boolean")
        ));
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("type", "object");
        author.put("required", List.of("name", "works", "features"));
        author.put("properties", Map.of(
                "name", Map.of("type", "string"),
                "works", Map.of("type", "array", "items", Map.of("type", "string")),
                "features", Map.of("type", "array", "items", feature)
        ));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "required", List.of("authors"),
                "properties", Map.of("authors", Map.of("type", "array", "items", author))
        );
        return Map.of("contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", schema));
    }

    LiteraryImageAnalysisResult parseResponse(String response, int imageNumber) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String output = findOutput(root);
            if (output == null || output.isBlank()) throw new LiteraryImageAnalysisException("AI response format is invalid.");
            JsonNode structured = objectMapper.readTree(stripFence(output));
            JsonNode authorsNode = structured == null ? null : structured.get("authors");
            if (authorsNode == null || !authorsNode.isArray()) throw new LiteraryImageAnalysisException("AI response format is invalid.");
            List<LiteraryImageAuthorDraft> authors = new ArrayList<>();
            for (JsonNode author : authorsNode) {
                String name = text(author, "name");
                if (name.isBlank()) throw new LiteraryImageAnalysisException("AI response contains an author without name.");
                List<String> works = new ArrayList<>();
                JsonNode worksNode = author.get("works");
                if (worksNode != null && worksNode.isArray()) for (JsonNode work : worksNode) if (work.isTextual() && !work.asText().isBlank()) works.add(work.asText().trim());
                List<LiteraryImageFeatureDraft> features = new ArrayList<>();
                JsonNode featuresNode = author.get("features");
                if (featuresNode != null && featuresNode.isArray()) for (JsonNode feature : featuresNode) {
                    String type = text(feature, "type").toUpperCase();
                    if (!List.of("WORK", "AUTHOR", "UNRESOLVED").contains(type)) throw new LiteraryImageAnalysisException("AI response contains an invalid feature type.");
                    String workTitle = feature.hasNonNull("workTitle") ? feature.get("workTitle").asText().trim() : null;
                    String content = text(feature, "content");
                    if (content.isBlank()) throw new LiteraryImageAnalysisException("AI response contains an empty feature.");
                    boolean needsReview = feature.path("needsReview").asBoolean() || "UNRESOLVED".equals(type);
                    features.add(new LiteraryImageFeatureDraft(type, workTitle, content, needsReview));
                }
                authors.add(new LiteraryImageAuthorDraft(name, works, features));
            }
            return new LiteraryImageAnalysisResult(imageNumber, authors);
        } catch (JsonProcessingException exception) {
            throw new LiteraryImageAnalysisException("AI response format is invalid.", exception);
        }
    }

    private String findOutput(JsonNode root) {
        for (JsonNode candidate : root.path("candidates")) for (JsonNode part : candidate.path("content").path("parts")) {
            if (part.has("text") && part.get("text").isTextual()) return part.get("text").asText();
        }
        return null;
    }

    private String stripFence(String value) {
        String result = value.trim();
        if (result.startsWith("```")) result = result.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "").replaceFirst("\\s*```$", "");
        return result;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText().trim() : "";
    }
}
