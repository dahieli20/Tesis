package com.datalakefilter.service;

import com.datalakefilter.dto.RiskThresholdConfigRequest;
import com.datalakefilter.dto.RiskThresholdConfigResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class RiskThresholdConfigService {

    private double cleanMax = 30.0;
    private double frontierMax = 60.0;

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
        if (cleanMax < 0 || frontierMax > 100) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Los rangos deben estar entre 0 y 100."
            );
        }

        if (cleanMax >= frontierMax) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "El límite de Data Lake limpio debe ser menor al límite de frontera."
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