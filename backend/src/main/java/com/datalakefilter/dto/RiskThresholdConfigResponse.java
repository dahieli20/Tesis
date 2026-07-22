package com.datalakefilter.dto;

public record RiskThresholdConfigResponse(
        double cleanMax,
        double frontierMax,
        String cleanRange,
        String frontierRange,
        String swampRange
) {
}