package com.koreanvocabquiz.vocabulary;

import java.util.List;

public interface VocabularyImageAnalysisClient {

    List<VocabularyImageAnalysisResult> extract(List<VocabularyImageFile> images);
}
