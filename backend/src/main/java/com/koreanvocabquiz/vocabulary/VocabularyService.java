package com.koreanvocabquiz.vocabulary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

    private final VocabularyRepository vocabularyRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
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
        Set<VocabularyDuplicateKey> uploadSuccessKeys = new HashSet<>();
        List<Vocabulary> vocabulariesToSave = new ArrayList<>();
        VocabularyCsvUploadResponse.Builder result = VocabularyCsvUploadResponse.builder(rows.size());

        for (CsvVocabularyRow row : rows) {
            List<String> validationErrors = validate(row);
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
                result.addSkipped(row.rowNumber(), "Duplicate row in the same CSV upload.");
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

    private List<String> validate(CsvVocabularyRow row) {
        List<String> errors = new ArrayList<>();

        if (row.word().isBlank()) {
            errors.add("word is required");
        }
        if (row.meaning().isBlank()) {
            errors.add("meaning is required");
        }
        if (row.category().isBlank()) {
            errors.add("category is required");
        } else if (!isValidCategory(row.category())) {
            errors.add("category must be one of " + Arrays.toString(VocabularyCategory.values()));
        }

        return errors;
    }

    private boolean isValidCategory(String category) {
        return Arrays.stream(VocabularyCategory.values())
                .anyMatch(value -> value.name().equals(category));
    }

    private record CsvVocabularyRow(
            int rowNumber,
            String word,
            String meaning,
            String category
    ) {
    }

    private record VocabularyDuplicateKey(
            String word,
            String meaning,
            VocabularyCategory category
    ) {
    }
}
