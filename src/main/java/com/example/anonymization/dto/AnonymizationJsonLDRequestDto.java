package com.example.anonymization.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(name="AnonymizationJsonLDRequestDto", description="DTO for JSON-LD anonymization request")
public class AnonymizationJsonLDRequestDto {

    @Schema(description = "The configuration URL")
    private String configurationUrl;

    @Schema(description = "Data to be anonymized")
    private JsonNode data;

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
