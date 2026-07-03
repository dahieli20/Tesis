package com.datalakefilter.dto;

public record CsvQualityResult(
        int totalRows,
        int totalColumns,
        double nullPercentage,
        double duplicateRowsPercentage,
        boolean hasDuplicateColumns,
        boolean hasDataRows
) {
}