package com.example.anonymization;

import com.example.anonymization.exceptions.AnonymizationException;
import com.example.anonymization.exceptions.OntologyException;
import com.example.anonymization.exceptions.RequestModelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OntologyException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(OntologyException ex) {
        log.error("OntologyException: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Error in ontology fetching or parsing");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(AnonymizationException.class)
    public ResponseEntity<ProblemDetail> handleAnonymizationException(AnonymizationException ex) {
        log.error("AnonymizationException: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Error during anonymization process");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    @ExceptionHandler(RequestModelException.class)
    public ResponseEntity<ProblemDetail> handleRequestModelException(RequestModelException ex) {
        log.error("RequestModelException: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid request model");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
