package com.koreanvocabquiz.literature;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/literature")
public class LiteratureController {

    private final LiteratureService service;

    public LiteratureController(LiteratureService service) { this.service = service; }

    @GetMapping("/authors")
    public List<LiteraryAuthorResponse> authors() { return service.authors(); }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public LiteraryAuthorResponse createAuthor(@Valid @RequestBody LiteraryAuthorRequest request) { return service.createAuthor(request); }

    @PutMapping("/authors/{id}")
    public LiteraryAuthorResponse updateAuthor(@PathVariable Long id, @Valid @RequestBody LiteraryAuthorRequest request) { return service.updateAuthor(id, request); }

    @DeleteMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable Long id) { service.deleteAuthor(id); }

    @GetMapping("/works")
    public List<LiteraryWorkResponse> works() { return service.works(); }

    @PostMapping("/works")
    @ResponseStatus(HttpStatus.CREATED)
    public LiteraryWorkResponse createWork(@Valid @RequestBody LiteraryWorkRequest request) { return service.createWork(request); }

    @PutMapping("/works/{id}")
    public LiteraryWorkResponse updateWork(@PathVariable Long id, @Valid @RequestBody LiteraryWorkRequest request) { return service.updateWork(id, request); }

    @DeleteMapping("/works/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWork(@PathVariable Long id) { service.deleteWork(id); }

    @GetMapping("/features")
    public List<LiteraryFeatureResponse> features() { return service.features(); }

    @PostMapping("/features")
    @ResponseStatus(HttpStatus.CREATED)
    public LiteraryFeatureResponse createFeature(@Valid @RequestBody LiteraryFeatureRequest request) { return service.createFeature(request); }

    @PutMapping("/features/{id}")
    public LiteraryFeatureResponse updateFeature(@PathVariable Long id, @Valid @RequestBody LiteraryFeatureRequest request) { return service.updateFeature(id, request); }

    @DeleteMapping("/features/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFeature(@PathVariable Long id) { service.deleteFeature(id); }

    @PostMapping(value = "/csv/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LiteraryCsvPreviewResponse previewCsv(@RequestPart("file") MultipartFile file) { return service.previewCsv(file); }

    @PostMapping("/csv/import")
    public LiteraryCsvImportResponse importCsv(@Valid @RequestBody LiteraryCsvImportRequest request) { return service.importCsv(request); }
}
