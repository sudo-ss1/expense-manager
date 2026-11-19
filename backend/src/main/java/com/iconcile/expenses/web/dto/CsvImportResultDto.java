package com.iconcile.expenses.web.dto;

import com.iconcile.expenses.domain.CsvExpenseParser;

import java.util.List;

public record CsvImportResultDto(int totalRows, int imported, int duplicatesSkipped, int failed,
                                 int anomaliesAmongImported, List<CsvExpenseParser.RowError> errors) {
}
