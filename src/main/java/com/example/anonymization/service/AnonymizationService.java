package com.example.anonymization.service;

import com.example.anonymization.dto.AnonymizationRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.anonymizer.Anonymization;
import org.apache.jena.rdf.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class AnonymizationService {

    /*
    TODO
    - Exception handling
    - Prefix handling
     */

    public static ResponseEntity<String> applyAnonymization(AnonymizationRequestDto request) {

        Map<String, Configuration> configurations = ConfigurationService.fetchConfig(request.getConfigurationUrl());
        Model model = getModel("exampleInputs/input_twoargument.ttl"); // TODO: extract from query
        List<Property> attributes = OntologyService.extractAttributesForAnonymization(model, configurations.keySet(), "testobject");
        Map<Resource, Map<Property, Literal>> data = OntologyService.extractDataFromModel(model, attributes, "testobject");
        OntologyService.deleteOldValues(model, attributes, "testobject");
        Map<Property, Map<Resource, Literal>> horizontalData = convertToHorizontalSchema(data, attributes);
        horizontalData.forEach(((property, resourceLiteralMap) ->
                Anonymization.anonmization(
                        configurations.get(property.getLocalName()),
                        model, property, resourceLiteralMap
                )));

        model.write(System.out, "TTL");

        return new ResponseEntity<>(
                "Response",
                HttpStatus.ACCEPTED
        );
    }

    private static Map<Property, Map<Resource, Literal>> convertToHorizontalSchema(
            Map<Resource, Map<Property, Literal>> data,
            List<Property> properties
    ) {
        Map<Property, Map<Resource, Literal>> propertyMap = new HashMap<>();
        properties.forEach(property -> propertyMap.put(property, new HashMap<>()));
        data.forEach((resource, value) -> value.forEach((property, literal) ->
                propertyMap.get(property).put(resource, literal)));
        return propertyMap;
    }

    /*
    Helper function until extraction from query body is implemented
     */
    private static Model getModel(String localPath) {
        Model model = ModelFactory.createDefaultModel();

        try (InputStream in = new FileInputStream(localPath)) {
            model.read(in, null, "TTL");
            model.write(System.out, "TTL");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }
}
