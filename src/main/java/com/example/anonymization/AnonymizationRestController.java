package com.example.anonymization;

import com.example.anonymization.dto.AnonymizationFlatJsonRequestDto;
import com.example.anonymization.dto.AnonymizationJsonLDRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.AnonymizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AnonymizationRestController {

    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Error",
                    content = @Content(mediaType = "application/json"))
    })
    @Operation(
            summary = "Anonymization of input data (JSON-LD)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnonymizationJsonLDRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Simple JSON-LD example",
                                            summary = "Simple JSON-LD request with one anonymization object",
                                            externalValue = "/examples/anonymization-request.json"
                                    ),
                                    @ExampleObject(
                                            name = "JSON-LD with two anonymization objects",
                                            summary = "JSON-LD request with two anonymization objects",
                                            externalValue = "/examples/anonymization-request-two-objects.json"
                                    ),
                                    @ExampleObject(
                                            name = "JSON-LD of object anonymization (address)",
                                            summary = "JSON-LD request for the anonymization of address objects",
                                            externalValue = "/examples/anonymization-request-object.json"
                                    )
                            }
                    )
            )
    )
    @PutMapping(value = "/api/anonymization",
            consumes = {"application/json", "application/ld+json"},
            produces = "application/json")
    public ResponseEntity<String> anonymization(@RequestBody AnonymizationJsonLDRequestDto anonymizationRequest) {
        return AnonymizationService.applyAnonymization(anonymizationRequest);
    }


    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Error",
                    content = @Content(mediaType = "application/json")),
    })
    @Operation(
            summary = "Anonymization of input data (flat JSON)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnonymizationFlatJsonRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Simple flat JSON example",
                                            summary = "Simple example with flat json input",
                                            externalValue = "/examples/anonymization-request-flat.json"
                                    ),
                                    @ExampleObject(
                                            name = "Flat JSON based on multiple objects",
                                            summary = "Example with flat json input based on two anonymization objects",
                                            externalValue = "/examples/anonymization-request-flat-two-objects.json"
                                    ),
                                    @ExampleObject(
                                            name = "Flat JSON for address anonymization",
                                            summary = "Example with flat json input for address anonymization",
                                            externalValue = "/examples/anonymization-request-flat-object.json"
                                    )
                            }
                    )
            )
    )
    @PutMapping("/api/anonymization/flatjson")
    public ResponseEntity<String> anonymizationFlat(
            @RequestBody AnonymizationFlatJsonRequestDto anonymizationRequest
    ) throws JsonProcessingException {
        return AnonymizationService.applyAnonymizationFlatJson(anonymizationRequest);
    }

}
