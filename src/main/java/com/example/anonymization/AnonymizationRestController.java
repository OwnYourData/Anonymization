package com.example.anonymization;

import com.example.anonymization.dto.AnonymizationFlatJsonRequestDto;
import com.example.anonymization.dto.AnonymizationJsonLDRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.AnonymizationService;
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

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Error",
                    content = @Content(mediaType = "application/json"))
    })
    @Operation(
            summary = "Anonymization of input data (flat JSON)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnonymizationJsonLDRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "flatjson-full",
                                            summary = "Full flat JSON request",
                                            externalValue = "/examples/anonymization-request.json"
                                    )
                            }
                    )
            )
    )
    @PutMapping(value = "/api/anonymization",
            consumes = {"application/json", "application/ld+json"},
            produces = "application/json")
    public ResponseEntity<String> anonymization(@RequestBody AnonymizationJsonLDRequestDto anonymizationRequest) {
        // logger and service call as in your code
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
                                            name = "flatjson-full",
                                            summary = "Full flat JSON request",
                                            externalValue = "/examples/anonymization-request-flat.json"
                                    )
                            }
                    )
            )
    )
    @PutMapping("/api/anonymization/flatjson")
    public ResponseEntity<String> anonymizationFlat(@RequestBody AnonymizationFlatJsonRequestDto anonymizationRequest) {

        logger.info("Received flat JSON anonymization request. Body: \n" + anonymizationRequest);

        return AnonymizationService.applyAnonymizationFlatJson(anonymizationRequest);
    }

}
