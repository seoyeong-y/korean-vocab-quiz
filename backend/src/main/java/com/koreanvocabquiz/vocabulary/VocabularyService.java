package com.koreanvocabquiz.vocabulary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.koreanvocabquiz.common.ResourceNotFoundException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class VocabularyService {

    private static final int MAX_IMAGE_COUNT = 5;
    private static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final long MAX_IMAGE_REQUEST_SIZE_BYTES = 50 * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final VocabularyRepository vocabularyRepository;
    private final VocabularyImageAnalysisClient vocabularyImageAnalysisClient;

    public VocabularyService(
            VocabularyRepository vocabularyRepository,
            VocabularyImageAnalysisClient vocabularyImageAnalysisClient
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.vocabularyImageAnalysisClient = vocabularyImageAnalysisClient;
    }

    @Transactional
    public VocabularyResponse create(VocabularyCreateRequest request) {
        Vocabulary vocabulary = new Vocabulary(
                request.word(),
                request.meaning(),
                request.resolvedCategory(),
                request.exampleSentence()
        );

        return VocabularyResponse.from(vocabularyRepository.save(vocabulary));
    }

    public List<VocabularyResponse> findAll() {
        return vocabularyRepository.findAll()
                .stream()
                .map(VocabularyResponse::from)
                .toList();
    }

    public VocabularyResponse findById(Long id) {
        return VocabularyResponse.from(getVocabulary(id));
    }

    @Transactional
    public VocabularyResponse update(Long id, VocabularyUpdateRequest request) {
        Vocabulary vocabulary = getVocabulary(id);
        vocabulary.update(
                request.word(),
                request.meaning(),
                request.category() == null ? vocabulary.getCategory() : request.category(),
                request.exampleSentence()
        );

        return VocabularyResponse.from(vocabulary);
    }

    @Transactional
    public void delete(Long id) {
        Vocabulary vocabulary = getVocabulary(id);
        vocabularyRepository.delete(vocabulary);
    }

    @Transactional
    public VocabularyCsvUploadResponse uploadCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("CSV file is required.");
        }

        List<CsvVocabularyRow> rows = parseCsv(file);
        return saveRows(rows, false, "Duplicate row in the same CSV upload.");
    }

    @Transactional
    public VocabularyCsvUploadResponse saveBatch(VocabularyBatchSaveRequest request) {
        if (request == null || request.items() == null) {
            throw new InvalidCsvException("Vocabulary items are required.");
        }

        List<BatchVocabularyRow> rows = new ArrayList<>();
        for (int index = 0; index < request.items().size(); index++) {
            VocabularyBatchItemRequest item = request.items().get(index);
            rows.add(new BatchVocabularyRow(
                    index + 1,
                    item == null || item.word() == null ? "" : item.word().trim(),
                    item == null || item.meaning() == null ? "" : item.meaning().trim(),
                    item == null || item.category() == null ? "" : item.category().trim().toUpperCase()
            ));
        }

        return saveRows(rows, true, "Duplicate row in the same request.");
    }

    public VocabularyImageExtractionResponse extractFromImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new VocabularyImageExtractionException("At least one image file is required.");
        }
        if (files.size() > MAX_IMAGE_COUNT) {
            throw new VocabularyImageExtractionException("Up to " + MAX_IMAGE_COUNT + " images can be uploaded at once.");
        }
        long totalSize = files.stream()
                .filter(file -> file != null)
                .mapToLong(MultipartFile::getSize)
                .sum();
        if (totalSize > MAX_IMAGE_REQUEST_SIZE_BYTES) {
            throw new VocabularyImageExtractionException("Total image upload size must be 50MB or less.");
        }

        List<VocabularyImageFile> images = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            validateImageFile(file);
            try {
                images.add(new VocabularyImageFile(index + 1, normalizeMediaType(file.getContentType()), file.getBytes()));
            } catch (IOException exception) {
                throw new VocabularyImageExtractionException("Failed to read uploaded image.", exception);
            }
        }

        List<VocabularyImageAnalysisResult> aiResults = vocabularyImageAnalysisClient.extract(images);
        if (aiResults.isEmpty()) {
            throw new VocabularyImageExtractionException("No vocabulary entries were found in the image.");
        }

        List<VocabularyImageCandidateResponse> candidates = validateAnalysisResults(aiResults, files.size());
        return new VocabularyImageExtractionResponse(candidates.size(), candidates);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VocabularyImageExtractionException("Image file is required.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new VocabularyImageExtractionException("Image file size must be 10MB or less.");
        }

        String mediaType = normalizeMediaType(file.getContentType());
        if (!SUPPORTED_IMAGE_TYPES.contains(mediaType)) {
            throw new VocabularyImageExtractionException("Only jpg, jpeg, png, and webp images are supported.");
        }
    }

    private String normalizeMediaType(String mediaType) {
        return mediaType == null ? "" : mediaType.toLowerCase();
    }

    private List<VocabularyImageCandidateResponse> validateAnalysisResults(
            List<VocabularyImageAnalysisResult> aiResults,
            int imageCount
    ) {
        Map<ImageWordKey, MergedImageAnalysisResult> mergedResults = new LinkedHashMap<>();
        for (VocabularyImageAnalysisResult result : aiResults) {
            validateAnalysisResult(result, imageCount);

            int imageNumber = result.imageNumber();
            String word = result.word().trim();
            ImageWordKey key = new ImageWordKey(imageNumber, word);
            mergedResults.computeIfAbsent(key, ignored -> new MergedImageAnalysisResult(
                    imageNumber,
                    word,
                    VocabularyCategory.valueOf(result.category()),
                    result.needsReview(),
                    result.confidence()
            )).merge(result);
        }

        List<VocabularyImageCandidateResponse> candidates = new ArrayList<>();
        int rowNumber = 1;

        for (MergedImageAnalysisResult result : mergedResults.values()) {
            candidates.add(new VocabularyImageCandidateResponse(
                    result.imageNumber,
                    rowNumber++,
                    result.word,
                    result.meaning(),
                    result.category,
                    result.needsReview,
                    result.confidence
            ));
        }

        return candidates;
    }

    private void validateAnalysisResult(VocabularyImageAnalysisResult result, int imageCount) {
        if (result.imageNumber() < 1 || result.imageNumber() > imageCount) {
            throw new VocabularyImageExtractionException("AI response contains an invalid imageNumber.");
        }
        if (result.word() == null || result.word().isBlank()) {
            throw new VocabularyImageExtractionException("AI response contains an item without word.");
        }
        if (result.meaning() == null || result.meaning().isBlank()) {
            throw new VocabularyImageExtractionException("AI response contains an item without meaning.");
        }
        if (result.category() == null || !isStudyCategory(result.category())) {
            throw new VocabularyImageExtractionException("AI response contains an invalid category.");
        }
    }

    private VocabularyCsvUploadResponse saveRows(
            List<? extends VocabularyRowData> rows,
            boolean studyCategoryOnly,
            String duplicateRowReason
    ) {
        Set<VocabularyDuplicateKey> uploadSuccessKeys = new HashSet<>();
        List<Vocabulary> vocabulariesToSave = new ArrayList<>();
        VocabularyCsvUploadResponse.Builder result = VocabularyCsvUploadResponse.builder(rows.size());

        for (VocabularyRowData row : rows) {
            List<String> validationErrors = validate(row, studyCategoryOnly);
            if (!validationErrors.isEmpty()) {
                result.addFailed(row.rowNumber(), String.join(", ", validationErrors));
                continue;
            }

            VocabularyCategory category = VocabularyCategory.valueOf(row.category());
            VocabularyDuplicateKey key = new VocabularyDuplicateKey(row.word(), row.meaning(), category);

            if (vocabularyRepository.existsByWordAndMeaningAndCategory(row.word(), row.meaning(), category)) {
                result.addSkipped(row.rowNumber(), "Already exists with the same word, meaning, and category.");
                continue;
            }

            if (!uploadSuccessKeys.add(key)) {
                result.addSkipped(row.rowNumber(), duplicateRowReason);
                continue;
            }

            vocabulariesToSave.add(new Vocabulary(row.word(), row.meaning(), category, null));
            result.addSuccess();
        }

        vocabularyRepository.saveAll(vocabulariesToSave);
        return result.build();
    }

    private Vocabulary getVocabulary(Long id) {
        return vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found. id=" + id));
    }

    private List<CsvVocabularyRow> parseCsv(MultipartFile file) {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
                );
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)
        ) {
            validateHeaders(parser);

            return parser.stream()
                    .map(record -> new CsvVocabularyRow(
                            (int) record.getRecordNumber() + 1,
                            read(record, "word"),
                            read(record, "meaning"),
                            read(record, "category").toUpperCase()
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new InvalidCsvException("Failed to read CSV file.");
        } catch (IllegalArgumentException exception) {
            throw new InvalidCsvException("CSV must contain word, meaning, and category columns.");
        }
    }

    private void validateHeaders(CSVParser parser) {
        Set<String> headers = parser.getHeaderMap().keySet();
        if (!headers.containsAll(List.of("word", "meaning", "category"))) {
            throw new InvalidCsvException("CSV must contain word, meaning, and category columns.");
        }
    }

    private String read(CSVRecord record, String header) {
        return record.get(header).trim();
    }

    private List<String> validate(VocabularyRowData row, boolean studyCategoryOnly) {
        List<String> errors = new ArrayList<>();

        if (row.word().isBlank()) {
            errors.add("word is required");
        }
        if (row.meaning().isBlank()) {
            errors.add("meaning is required");
        }
        if (row.category().isBlank()) {
            errors.add("category is required");
        } else if (studyCategoryOnly && !isStudyCategory(row.category())) {
            errors.add("category must be one of " + Arrays.toString(studyCategories()));
        } else if (!isValidCategory(row.category())) {
            errors.add("category must be one of " + Arrays.toString(VocabularyCategory.values()));
        }

        return errors;
    }

    private boolean isValidCategory(String category) {
        return Arrays.stream(VocabularyCategory.values())
                .anyMatch(value -> value.name().equals(category));
    }

    private boolean isStudyCategory(String category) {
        return Arrays.stream(studyCategories())
                .anyMatch(value -> value.name().equals(category));
    }

    static VocabularyCategory[] studyCategories() {
        return new VocabularyCategory[] {
                VocabularyCategory.NATIVE_KOREAN,
                VocabularyCategory.SINO_KOREAN,
                VocabularyCategory.LOANWORD,
                VocabularyCategory.PROVERB,
                VocabularyCategory.IDIOM
        };
    }

    private interface VocabularyRowData {
        int rowNumber();

        String word();

        String meaning();

        String category();
    }

    private record CsvVocabularyRow(
            int rowNumber,
            String word,
            String meaning,
            String category
    ) implements VocabularyRowData {
    }

    private record BatchVocabularyRow(
            int rowNumber,
            String word,
            String meaning,
            String category
    ) implements VocabularyRowData {
    }

    private record ImageWordKey(
            int imageNumber,
            String word
    ) {
    }

    private static final class MergedImageAnalysisResult {

        private final int imageNumber;
        private final String word;
        private final StringBuilder meaning = new StringBuilder();
        private final Set<String> meanings = new HashSet<>();
        private VocabularyCategory category;
        private boolean needsReview;
        private Double confidence;

        private MergedImageAnalysisResult(
                int imageNumber,
                String word,
                VocabularyCategory category,
                boolean needsReview,
                Double confidence
        ) {
            this.imageNumber = imageNumber;
            this.word = word;
            this.category = category;
            this.needsReview = needsReview;
            this.confidence = confidence;
        }

        private void merge(VocabularyImageAnalysisResult result) {
            String nextMeaning = result.meaning().trim();
            if (meanings.add(nextMeaning)) {
                if (!meaning.isEmpty()) {
                    meaning.append(System.lineSeparator());
                }
                meaning.append(nextMeaning);
            }

            VocabularyCategory nextCategory = VocabularyCategory.valueOf(result.category());
            if (category != nextCategory) {
                needsReview = true;
            }

            needsReview = needsReview || result.needsReview();
            confidence = lowerConfidence(confidence, result.confidence());
        }

        private String meaning() {
            return meaning.toString();
        }

        private Double lowerConfidence(Double current, Double next) {
            if (current == null || next == null) {
                return null;
            }
            return Math.min(current, next);
        }
    }

    private record VocabularyDuplicateKey(
            String word,
            String meaning,
            VocabularyCategory category
    ) {
    }
}
