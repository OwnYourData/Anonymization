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
}