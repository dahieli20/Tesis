package com.datalakefilter.dto;

import java.util.List;

public record DataLakeAuditResponse(
        int totalFiles,
        int rawFiles,
        int reviewFiles,
        int rejectedFiles,
        double stateRiskPercentage,
        double rawQualityRiskPercentage,
        double rawDuplicationRiskPercentage,
        double globalRiskPercentage,
        String classification,
        String message,
        List<AuditFactorResponse> factors
) {
}