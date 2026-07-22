package com.datalakefilter.dto;

public record RiskThresholdConfigRequest(
        double cleanMax,
        double frontierMax
) {
}