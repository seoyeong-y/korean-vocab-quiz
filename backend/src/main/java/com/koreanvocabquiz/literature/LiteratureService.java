package com.koreanvocabquiz.literature;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
public class LiteratureService {

    private static final long MAX_CSV_BYTES = 5 * 1024 * 1024;
    private static final int MAX_FEATURE_LENGTH = 400;
    private static final Set<String> CSV_HEADERS = Set.of("author", "work", "feature", "feature_type");

    private final LiteraryAuthorRepository authorRepository;
    private final LiteraryWorkRepository workRepository;
    private final LiteraryFeatureRepository featureRepository;

    public LiteratureService(
            LiteraryAuthorRepository authorRepository,
            LiteraryWorkRepository workRepository,
            LiteraryFeatureRepository featureRepository
    ) {
        this.authorRepository = authorRepository;
        this.workRepository = workRepository;
        this.featureRepository = featureRepository;
    }

    public List<LiteraryAuthorResponse> authors() {
        return authorRepository.findAllByOrderByNameAsc().stream().map(LiteraryAuthorResponse::from).toList();
    }

    public List<LiteraryWorkResponse> works() {
        return workRepository.findAllByOrderByTitleAsc().stream().map(LiteraryWorkResponse::from).toList();
    }

    public List<LiteraryFeatureResponse> features() {
        return featureRepository.findAllByOrderByIdAsc().stream().map(LiteraryFeatureResponse::from).toList();
    }

    @Transactional
    public LiteraryAuthorResponse createAuthor(LiteraryAuthorRequest request) {
        String name = clean(request.name());
        if (authorRepository.existsByName(name)) {
            throw new LiteratureValidationException("Author already exists: " + name);
        }
        return LiteraryAuthorResponse.from(authorRepository.save(new LiteraryAuthor(name)));
    }

    @Transactional
    public LiteraryAuthorResponse updateAuthor(Long id, LiteraryAuthorRequest request) {
        LiteraryAuthor author = author(id);
        String name = clean(request.name());
        if (authorRepository.existsByNameAndIdNot(name, id)) {
            throw new LiteratureValidationException("Author already exists: " + name);
        }
        author.update(name);
        return LiteraryAuthorResponse.from(author);
    }

    @Transactional
    public void deleteAuthor(Long id) {
        authorRepository.delete(author(id));
    }

    @Transactional
    public LiteraryWorkResponse createWork(LiteraryWorkRequest request) {
        LiteraryAuthor author = author(request.authorId());
        String title = clean(request.title());
        if (workRepository.existsByAuthorIdAndTitle(author.getId(), title)) {
            throw new LiteratureValidationException("Work already exists for this author: " + title);
        }
        return LiteraryWorkResponse.from(workRepository.save(new LiteraryWork(author, title)));
    }

    @Transactional
    public LiteraryWorkResponse updateWork(Long id, LiteraryWorkRequest request) {
        LiteraryWork work = work(id);
        LiteraryAuthor author = author(request.authorId());
        String title = clean(request.title());
        if (workRepository.existsByAuthorIdAndTitleAndIdNot(author.getId(), title, id)) {
            throw new LiteratureValidationException("Work already exists for this author: " + title);
        }
        work.update(author, title);
        return LiteraryWorkResponse.from(work);
    }

    @Transactional
    public void deleteWork(Long id) {
        workRepository.delete(work(id));
    }

    @Transactional
    public LiteraryFeatureResponse createFeature(LiteraryFeatureRequest request) {
        LiteraryFeatureInput input = validateFeature(request.authorId(), request.workId(), request.type(), request.content());
        if (featureExists(input)) {
            throw new LiteratureValidationException("Feature already exists.");
        }
        return LiteraryFeatureResponse.from(featureRepository.save(new LiteraryFeature(input.author(), input.work(), input.type(), input.content())));
    }

    @Transactional
    public LiteraryFeatureResponse updateFeature(Long id, LiteraryFeatureRequest request) {
        LiteraryFeature feature = feature(id);
        LiteraryFeatureInput input = validateFeature(request.authorId(), request.workId(), request.type(), request.content());
        if (featureExistsExcept(input, id)) {
            throw new LiteratureValidationException("Feature already exists.");
        }
        feature.update(input.author(), input.work(), input.type(), input.content());
        return LiteraryFeatureResponse.from(feature);
    }

    @Transactional
    public void deleteFeature(Long id) {
        featureRepository.delete(feature(id));
    }

    public LiteraryCsvPreviewResponse previewCsv(MultipartFile file) {
        List<LiteraryCsvRowResponse> rows = parseCsv(file);
        return new LiteraryCsvPreviewResponse(rows.size(), rows);
    }

    @Transactional
    public LiteraryCsvImportResponse importCsv(LiteraryCsvImportRequest request) {
        List<LiteraryCsvRowResponse> results = new ArrayList<>();
        int success = 0;
        int skipped = 0;
        int failed = 0;
        for (LiteraryCsvRowRequest row : request.rows()) {
            if (!row.selected()) {
                continue;
            }
            try {
                RowValidation validation = validateRow(row.author(), row.work(), row.feature(), row.featureType());
                if (!validation.valid()) {
                    failed++;
                    results.add(result(row, "ERROR", validation.reason()));
                    continue;
                }
                ImportResult imported = saveRow(validation);
                if (imported.skipped()) {
                    skipped++;
                    results.add(result(row, "DUPLICATE", "이미 존재하는 데이터입니다."));
                } else {
                    success++;
                    results.add(result(row, "NORMAL", "저장되었습니다."));
                }
            } catch (RuntimeException exception) {
                failed++;
                results.add(result(row, "ERROR", exception.getMessage()));
            }
        }
        return new LiteraryCsvImportResponse(success + skipped + failed, success, skipped, failed, results);
    }

    private List<LiteraryCsvRowResponse> parseCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new LiteratureValidationException("CSV file is required.");
        if (file.getSize() > MAX_CSV_BYTES) throw new LiteratureValidationException("CSV file must be 5MB or smaller.");
        try {
            String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (csv.startsWith("\uFEFF")) csv = csv.substring(1);
            try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(false).build().parse(new StringReader(csv))) {
                Set<String> headers = new LinkedHashSet<>(parser.getHeaderMap().keySet());
                if (!headers.equals(CSV_HEADERS)) throw new LiteratureValidationException("CSV header must be author,work,feature,feature_type.");
                List<LiteraryCsvRowResponse> rows = new ArrayList<>();
                String currentAuthor = null;
                Set<String> seenWorks = new HashSet<>();
                Set<String> seenFeatures = new HashSet<>();
                for (CSVRecord record : parser) {
                    String rawAuthor = cleanNullable(record.get("author"));
                    if (rawAuthor != null) currentAuthor = rawAuthor;
                    String author = currentAuthor;
                    String work = cleanNullable(record.get("work"));
                    String feature = cleanNullable(record.get("feature"));
                    LiteratureFeatureType type = parseFeatureType(cleanNullable(record.get("feature_type")));
                    RowValidation validation = validateRow(author, work, feature, type);
                    String status = validation.status();
                    String reason = validation.reason();
                    if (validation.valid() && isDuplicate(validation, seenWorks, seenFeatures)) {
                        status = "DUPLICATE";
                        reason = "같은 CSV 또는 DB에 이미 존재하는 데이터입니다.";
                    }
                    if (validation.valid()) markSeen(validation, seenWorks, seenFeatures);
                    rows.add(new LiteraryCsvRowResponse((int) record.getRecordNumber(), author, work, feature, type, status, reason));
                }
                return rows;
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new LiteratureValidationException("Malformed CSV file.");
        }
    }

    private RowValidation validateRow(String author, String work, String feature, LiteratureFeatureType type) {
        if (author == null) return new RowValidation(false, "ERROR", "author를 확인할 수 없습니다.", author, work, feature, type);
        if (work == null && feature == null) return new RowValidation(false, "ERROR", "work 또는 feature가 필요합니다.", author, work, feature, type);
        if (feature == null && type != null) return new RowValidation(false, "ERROR", "feature_type은 feature와 함께 사용해야 합니다.", author, work, feature, type);
        if (feature != null && feature.length() > MAX_FEATURE_LENGTH) return new RowValidation(false, "ERROR", "feature는 400자 이하여야 합니다.", author, work, feature, type);
        if (feature != null && type == null) return new RowValidation(true, "NEEDS_REVIEW", "feature_type 확인 필요", author, work, feature, type);
        if (type == LiteratureFeatureType.WORK && work == null) return new RowValidation(false, "ERROR", "WORK 특징에는 work가 필요합니다.", author, work, feature, type);
        if (type == LiteratureFeatureType.AUTHOR && work != null) return new RowValidation(false, "ERROR", "AUTHOR 특징의 work는 비워야 합니다.", author, work, feature, type);
        return new RowValidation(true, "NORMAL", "정상", author, work, feature, type);
    }

    private ImportResult saveRow(RowValidation row) {
        LiteraryAuthor author = authorRepository.findByName(row.author()).orElseGet(() -> authorRepository.save(new LiteraryAuthor(row.author())));
        boolean changed = author.getId() == null;
        LiteraryWork work = null;
        if (row.work() != null) {
            work = workRepository.findByAuthorIdAndTitle(author.getId(), row.work()).orElse(null);
            if (work == null) {
                work = workRepository.save(new LiteraryWork(author, row.work()));
                changed = true;
            }
        }
        if (row.feature() != null) {
            if (row.type() == null) throw new LiteratureValidationException("feature_type 확인 필요");
            if (featureExists(new LiteraryFeatureInput(author, work, row.type(), row.feature()))) {
                if (!changed) return new ImportResult(true);
            } else {
                featureRepository.save(new LiteraryFeature(author, work, row.type(), row.feature()));
                changed = true;
            }
        }
        return new ImportResult(!changed);
    }

    private boolean isDuplicate(RowValidation row, Set<String> seenWorks, Set<String> seenFeatures) {
        boolean duplicateWork = row.work() != null && (workRepository.existsByAuthorIdAndTitle(authorId(row.author()), row.work()) || seenWorks.contains(workKey(row.author(), row.work())));
        boolean duplicateFeature = row.feature() != null && row.type() != null
                && (featureExists(row.author(), row.work(), row.type(), row.feature()) || seenFeatures.contains(featureKey(row.author(), row.work(), row.type(), row.feature())));
        return row.feature() == null ? duplicateWork : duplicateFeature && (row.work() == null || duplicateWork);
    }

    private void markSeen(RowValidation row, Set<String> seenWorks, Set<String> seenFeatures) {
        if (row.work() != null) seenWorks.add(workKey(row.author(), row.work()));
        if (row.feature() != null && row.type() != null) seenFeatures.add(featureKey(row.author(), row.work(), row.type(), row.feature()));
    }

    private LiteraryFeatureInput validateFeature(Long authorId, Long workId, LiteratureFeatureType type, String content) {
        LiteraryAuthor author = author(authorId);
        LiteraryWork work = workId == null ? null : work(workId);
        String cleanContent = clean(content);
        if (cleanContent.length() > MAX_FEATURE_LENGTH) throw new LiteratureValidationException("Feature must be 400 characters or fewer.");
        if (type == LiteratureFeatureType.WORK && work == null) throw new LiteratureValidationException("WORK feature requires a work.");
        if (type == LiteratureFeatureType.AUTHOR && work != null) throw new LiteratureValidationException("AUTHOR feature cannot reference a work.");
        if (work != null && !work.getAuthor().getId().equals(author.getId())) throw new LiteratureValidationException("Work does not belong to the selected author.");
        return new LiteraryFeatureInput(author, work, type, cleanContent);
    }

    private boolean featureExists(LiteraryFeatureInput input) { return featureExists(input.author().getId(), input.work() == null ? null : input.work().getId(), input.type(), input.content()); }
    private boolean featureExistsExcept(LiteraryFeatureInput input, Long id) { return featureRepository.existsByAuthorIdAndWorkIdAndContentAndIdNot(input.author().getId(), input.work() == null ? null : input.work().getId(), input.content(), id); }
    private boolean featureExists(String author, String work, LiteratureFeatureType type, String content) { return featureExists(authorId(author), work == null ? null : workId(author, work), type, content); }
    private boolean featureExists(Long authorId, Long workId, LiteratureFeatureType type, String content) { return featureRepository.existsByAuthorIdAndWorkIdAndContent(authorId, workId, content); }

    private Long authorId(String name) { return authorRepository.findByName(name).map(LiteraryAuthor::getId).orElse(-1L); }
    private Long workId(String author, String title) { return workRepository.findByAuthorIdAndTitle(authorId(author), title).map(LiteraryWork::getId).orElse(-1L); }
    private LiteraryAuthor author(Long id) { return authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Literary author not found. id=" + id)); }
    private LiteraryAuthor author(String name) { return authorRepository.findByName(name).orElseThrow(() -> new LiteratureValidationException("Author not found: " + name)); }
    private LiteraryWork work(Long id) { return workRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Literary work not found. id=" + id)); }
    private LiteraryFeature feature(Long id) { return featureRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Literary feature not found. id=" + id)); }
    private String clean(String value) { if (value == null || value.trim().isEmpty()) throw new LiteratureValidationException("Value must not be blank."); return value.trim(); }
    private String cleanNullable(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private LiteratureFeatureType parseFeatureType(String value) { return value == null ? null : switch (value.toUpperCase()) { case "WORK" -> LiteratureFeatureType.WORK; case "AUTHOR" -> LiteratureFeatureType.AUTHOR; default -> throw new LiteratureValidationException("Invalid feature_type: " + value); }; }
    private String workKey(String author, String work) { return author + "\u0000" + work; }
    private String featureKey(String author, String work, LiteratureFeatureType type, String content) { return author + "\u0000" + work + "\u0000" + type + "\u0000" + content; }
    private LiteraryCsvRowResponse result(LiteraryCsvRowRequest row, String status, String reason) { return new LiteraryCsvRowResponse(row.rowNumber(), row.author(), row.work(), row.feature(), row.featureType(), status, reason); }

    private record LiteraryFeatureInput(LiteraryAuthor author, LiteraryWork work, LiteratureFeatureType type, String content) {}
    private record RowValidation(boolean valid, String status, String reason, String author, String work, String feature, LiteratureFeatureType type) {}
    private record ImportResult(boolean skipped) {}
}
