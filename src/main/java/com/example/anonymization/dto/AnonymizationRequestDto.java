package com.example.anonymization.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AnonymizationRequestDto {

    private String configurationUrl;
    private JsonNode data;
}
