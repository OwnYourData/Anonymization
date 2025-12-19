package com.example.anonymization;

import com.example.anonymization.exceptions.AnonymizationException;
import com.example.anonymization.exceptions.OntologyException;
import com.example.anonymization.exceptions.RequestModelException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OntologyException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(OntologyException ex) {
        logger.error("OntologyException: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Error in ontology fetching or parsing");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(pd.getStatus()).body(pd);
    }

    @ExceptionHandler(AnonymizationException.class)
    public ResponseEntity<ProblemDetail> handleAnonymizationException(AnonymizationException ex) {
        logger.error("AnonymizationException: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Error during anonymization process");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(pd.getStatus()).body(pd);
    }

    @ExceptionHandler(RequestModelException.class)
    public ResponseEntity<ProblemDetail> handleRequestModelException(RequestModelException ex) {
        logger.error("RequestModelException: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid request model");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(pd.getStatus()).body(pd);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ProblemDetail> handleJsonException(JsonProcessingException ex) {
        logger.error("JsonProcessingException: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Error creation Json output");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(pd.getStatus()).body(pd);
    }
}
