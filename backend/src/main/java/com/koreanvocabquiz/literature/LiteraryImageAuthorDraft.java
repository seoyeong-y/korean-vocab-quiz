package com.koreanvocabquiz.literature;

import java.util.List;

public record LiteraryImageAuthorDraft(String name, List<String> works, List<LiteraryImageFeatureDraft> features) {}
