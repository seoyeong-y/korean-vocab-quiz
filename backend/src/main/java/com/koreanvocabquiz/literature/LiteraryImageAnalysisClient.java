package com.koreanvocabquiz.literature;

import java.util.List;

public interface LiteraryImageAnalysisClient {
    List<LiteraryImageAnalysisResult> extract(List<LiteraryImageFile> images);
}
