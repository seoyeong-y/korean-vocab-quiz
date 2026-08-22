package com.koreanvocabquiz.vocabulary;

import java.util.List;

public record VocabularyImageExtractionResponse(
        int totalCount,
        List<VocabularyImageCandidateResponse> items
) {
}
