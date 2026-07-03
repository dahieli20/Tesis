package com.datalakefilter.dto;

public record AuditFactorResponse(
        String name,
        double value,
        double weight,
        double contribution,
        String description
) {
}