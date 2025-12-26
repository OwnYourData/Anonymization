package com.example.anonymization.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(name="AnonymizationJsonLDRequestDto", description="DTO for JSON-LD anonymization request")
public class AnonymizationJsonLDRequestDto {

    @Schema(
            description = "The configuration URL",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "configurationUrl is mandatory")
    private String configurationUrl;

    @Schema(
            description = "Data to be anonymized",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "data is mandatory")
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

    @Schema(
            description = "Random seed for anonymization processes to ensure reproducibility.",
            defaultValue = "current system time in milliseconds"
    )
    private long randomSeed = System.currentTimeMillis();
}
