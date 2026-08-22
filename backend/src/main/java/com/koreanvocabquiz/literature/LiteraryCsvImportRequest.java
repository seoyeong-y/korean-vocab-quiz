package com.koreanvocabquiz.literature;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record LiteraryCsvImportRequest(@NotEmpty List<@Valid LiteraryCsvRowRequest> rows) {}
