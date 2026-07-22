package com.datalakefilter.controller;

import com.datalakefilter.dto.RiskThresholdConfigRequest;
import com.datalakefilter.dto.RiskThresholdConfigResponse;
import com.datalakefilter.service.RiskThresholdConfigService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datasets/audit/config")
@CrossOrigin(origins = "http://localhost:4200")
public class RiskThresholdConfigController {

    private final RiskThresholdConfigService riskThresholdConfigService;

    public RiskThresholdConfigController(RiskThresholdConfigService riskThresholdConfigService) {
        this.riskThresholdConfigService = riskThresholdConfigService;
    }

    @GetMapping("/thresholds")
    public RiskThresholdConfigResponse getThresholds() {
        return riskThresholdConfigService.getConfig();
    }

    @PutMapping("/thresholds")
    public RiskThresholdConfigResponse updateThresholds(
            @RequestBody RiskThresholdConfigRequest request
    ) {
        return riskThresholdConfigService.updateConfig(request);
    }
}