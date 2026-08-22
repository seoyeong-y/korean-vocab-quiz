package com.koreanvocabquiz.vocabulary;

import java.util.List;

public record VocabularyBatchSaveRequest(
        List<VocabularyBatchItemRequest> items
) {
}
