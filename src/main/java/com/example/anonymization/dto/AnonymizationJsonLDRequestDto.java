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
}
