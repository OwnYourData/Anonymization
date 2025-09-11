package com.example.anonymization;

import com.example.anonymization.dto.AnonymizationRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.AnonymizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
                    content = @Content(mediaType = "application/json")),
    })
    @Operation(summary = "Anonymization of input data")
    @PutMapping("/api/anonymization")
    public ResponseEntity<String> reasoning(@RequestBody AnonymizationRequestDto anonymizationRequest) {

        logger.info("Received request. Body: \n" + anonymizationRequest);

        return AnonymizationService.applyAnonymization(anonymizationRequest);
    }

}
