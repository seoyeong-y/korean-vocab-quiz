package com.koreanvocabquiz.literature;

import java.util.List;

public record LiteraryCsvImportResponse(
        int totalCount, int successCount, int skippedCount, int failedCount, List<LiteraryCsvRowResponse> rows
) {}
