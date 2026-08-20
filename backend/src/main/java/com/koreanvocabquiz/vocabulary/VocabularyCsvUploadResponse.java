package com.koreanvocabquiz.vocabulary;

import java.util.ArrayList;
import java.util.List;

public record VocabularyCsvUploadResponse(
        int totalCount,
        int successCount,
        int skippedCount,
        int failedCount,
        List<VocabularyCsvRowResult> skippedRows,
        List<VocabularyCsvRowResult> failedRows
) {
    public static Builder builder(int totalCount) {
        return new Builder(totalCount);
    }

    public static class Builder {

        private final int totalCount;
        private int successCount;
        private final List<VocabularyCsvRowResult> skippedRows = new ArrayList<>();
        private final List<VocabularyCsvRowResult> failedRows = new ArrayList<>();

        private Builder(int totalCount) {
            this.totalCount = totalCount;
        }

        public void addSuccess() {
            successCount++;
        }

        public void addSkipped(int rowNumber, String reason) {
            skippedRows.add(new VocabularyCsvRowResult(rowNumber, reason));
        }

        public void addFailed(int rowNumber, String reason) {
            failedRows.add(new VocabularyCsvRowResult(rowNumber, reason));
        }

        public VocabularyCsvUploadResponse build() {
            return new VocabularyCsvUploadResponse(
                    totalCount,
                    successCount,
                    skippedRows.size(),
                    failedRows.size(),
                    List.copyOf(skippedRows),
                    List.copyOf(failedRows)
            );
        }
    }
}
