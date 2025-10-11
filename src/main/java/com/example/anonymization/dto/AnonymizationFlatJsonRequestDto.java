package com.example.anonymization.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AnonymizationFlatJsonRequestDto {

    @Schema(
            description = "The configuration URL",
            example = "https://soya.ownyourdata.eu/AnonymisationDemo"
    )
    private String configurationUrl;

    @Schema(
            description = "Prefix of the anonymization properties",
            example = "https://soya.ownyourdata.eu/AnonymisationDemo/"
    )
    private String prefix;

    @Schema(
            description = "Data to be anonymized",
            example = "[{\"Name\": \"Linda Schäfer\", \"Adresse\": {\"Detail\": \"Martin-Fleck-Straße 0/1\", \"Zip\": \"9788\", \"State\": \"Wien\", \"City\": \"Hainfeld\", \"Country\": \"Austria\"}, \"Latitude\": 26.042505, \"Longitude\": -21.316927, \"Geburtsdatum\": \"1956-09-17\", \"Gewicht\": 76.58910075827146, \"Körpergröße\": 161.43868377230694}]"
    )
    private List<Map<String, Object>> data;
}