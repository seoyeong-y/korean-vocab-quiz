package com.koreanvocabquiz.literature;

import java.util.List;

public record LiteraryCsvPreviewResponse(int totalCount, List<LiteraryCsvRowResponse> rows) {}
