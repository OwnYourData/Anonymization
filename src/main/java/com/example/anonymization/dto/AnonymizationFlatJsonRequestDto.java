package com.example.anonymization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AnonymizationFlatJsonRequestDto {

        @Schema(description = "The configuration URL")
        @NotBlank(message = "configurationUrl is mandatory")
        private String configurationUrl;

        @Schema(description = "Prefix of the anonymization properties")
        @NotBlank(message = "Prefix is mandatory")
        private String prefix;

        @Schema(description = "Data to be anonymized")
        @NotEmpty(message = "data is mandatory")
        private List<Map<String, Object>> data;

        @Schema(description = "If true, KPIs will be calculated and included in the response.", defaultValue = "true")
        private boolean calculateKpi = true;

        @Schema(description = "If true, the original (non-anonymized) input data is also included in the response " +
                        "for analysis/verification purposes. Use with care.", defaultValue = "false")
        private boolean includeOriginalData = false;

        @Schema(description = "Random seed for anonymization processes to ensure reproducibility.", defaultValue = "current system time in milliseconds")
        private long randomSeed = System.currentTimeMillis();

        @Schema(description = "If true, adjusted attributes will be used for anonymization.", defaultValue = "true")
        private boolean useAdjustedAttributes = true;

        @AssertTrue(message = "useAdjustedAttributes must be true when includeOriginalData is true")
        @Schema(hidden = true)
        public boolean isValidAttributeConfiguration() {
                if (includeOriginalData) {
                        return useAdjustedAttributes;
                }
                return true;
        }
}