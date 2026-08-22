package com.koreanvocabquiz.literature;

import java.util.List;

public record LiteraryImageExtractionResponse(int totalCount, List<LiteraryImageCandidateResponse> rows) {}
