package com.example.anonymization.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class AnonymizationRequestDto {

    private String configurationUrl;
    private List<Map<String, Object>> data;
}
