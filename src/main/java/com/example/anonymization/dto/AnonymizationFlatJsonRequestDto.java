package com.example.anonymization.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AnonymizationFlatJsonRequestDto {

    @Schema(description = "The configuration URL")
    private String configurationUrl;

    @Schema(description = "Prefix of the anonymization properties")
    private String prefix;

    @Schema(description = "Data to be anonymized")
    private List<Map<String, Object>> data;

    @Schema(
            description = "If true, KPIs will be calculated and included in the response.",
            defaultValue = "true"
    )
    private boolean calculateKpi = true;

    @Schema(
            description = "If true, the original (non-anonymized) input data is also included in the response " +
                    "for analysis/verification purposes. Use with care.",
            defaultValue = "false"
    )
    private boolean includeOriginalData = false;
}