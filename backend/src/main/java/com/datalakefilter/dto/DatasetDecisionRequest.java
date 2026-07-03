package com.datalakefilter.dto;

public record DatasetDecisionRequest(
        String fileName,
        String path,
        String fileHash,
        Double partialMatchPercentage,
        String matchedWith,
        CsvQualityResult quality
) {
}
