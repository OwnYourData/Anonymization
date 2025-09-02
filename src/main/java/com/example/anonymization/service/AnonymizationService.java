package com.example.anonymization.service;

import com.example.anonymization.dto.AnonymizationRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.anonymizer.Anonymization;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.StringReader;
import java.util.*;

public class AnonymizationService {

    /*
    TODO
    - Exception handling
     */

    public static ResponseEntity<String> applyAnonymization(AnonymizationRequestDto request) {

        Map<Resource, Map<Property, Configuration>> anonymizationObjects = ConfigurationService.fetchConfig(request.getConfigurationUrl());
        Model model = getModel(request.getData());
        anonymizationObjects.forEach((object, config) -> applyAnonymizationForObject(object, config, model));
        model.write(System.out, "TTL");

        return new ResponseEntity<>(
                "Response",
                HttpStatus.ACCEPTED
        );
    }

    private static void applyAnonymizationForObject(Resource resource, Map<Property, Configuration> configurations, Model model) {
        List<Property> attributes = OntologyService.extractAttributesForAnonymization(model, configurations.keySet(), resource);
        Map<Resource, Map<Property, Literal>> data = OntologyService.extractDataFromModel(model, attributes, resource);
        OntologyService.deleteOldValues(model, attributes, resource);
        Map<Property, Map<Resource, Literal>> horizontalData = convertToHorizontalSchema(data, attributes);
        int nrAnonymizeAttributes = getNumberOfAnonymizingAttributes(configurations, attributes);
        horizontalData.forEach(((property, resourceLiteralMap) ->
                Anonymization.anonymization(
                        configurations.get(property),
                        model,
                        property,
                        resourceLiteralMap,
                        nrAnonymizeAttributes
                )));
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

    private static int getNumberOfAnonymizingAttributes(Map<Property, Configuration> configs, List<Property> attributes) {
        return (int) attributes.stream()
                .map(configs::get)
                .map(Configuration::getAnonymization)
                .filter(anonymization -> List.of("generalization", "randomization").contains(anonymization))
                .count();
    }

    private static Model getModel(JsonNode data) {
        String jsonLdString = data.toString();
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, new StringReader(jsonLdString), null, RDFLanguages.JSONLD);
        return model;
    }
}
