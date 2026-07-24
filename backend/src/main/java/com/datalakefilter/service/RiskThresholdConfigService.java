package com.datalakefilter.service;

import com.datalakefilter.dto.RiskThresholdConfigRequest;
import com.datalakefilter.dto.RiskThresholdConfigResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class RiskThresholdConfigService {

    private double cleanMax = 25.0;
    private double frontierMax = 50.0;

    public RiskThresholdConfigResponse getConfig() {
        return buildResponse();
    }

    public RiskThresholdConfigResponse updateConfig(RiskThresholdConfigRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "La configuración de rangos es obligatoria."
            );
        }

        validate(request.cleanMax(), request.frontierMax());

        this.cleanMax = request.cleanMax();
        this.frontierMax = request.frontierMax();

        return buildResponse();
    }

    public double getCleanMax() {
        return cleanMax;
    }

    public double getFrontierMax() {
        return frontierMax;
    }

    private void validate(double cleanMax, double frontierMax) {
        if (cleanMax < 0 || cleanMax >= 100 || frontierMax <= 0 || frontierMax >= 100) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Los umbrales deben cumplir: 0 <= saludable < frontera < 100."
            );
        }

        if (cleanMax >= frontierMax) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "El límite saludable debe ser menor que el límite frontera."
            );
        }
    }

    private RiskThresholdConfigResponse buildResponse() {
        return new RiskThresholdConfigResponse(
                cleanMax,
                frontierMax,
                "0% - " + cleanMax + "%",
                (cleanMax + 0.01) + "% - " + frontierMax + "%",
                (frontierMax + 0.01) + "% - 100%"
        );
    }
}