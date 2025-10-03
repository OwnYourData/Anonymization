package com.example.anonymization.service;

import com.example.anonymization.dto.AnonymizationRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.anonymizer.Anonymization;
import com.example.anonymization.service.data.QueryService;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Service
public class AnonymizationService {

    private static final Logger logger = LogManager.getLogger(AnonymizationService.class);

    public static ResponseEntity<String> applyAnonymization(AnonymizationRequestDto request) {

        // TODO Exception handling in whole service

        try {
            Map<Resource, Map<Property, Configuration>> anonymizationObjects = ConfigurationService.fetchConfig(request.getConfigurationUrl());
            Model model = getModel(request.getData());
            anonymizationObjects.forEach((object, config) -> applyAnonymizationForObject(object, config, model));
            StringWriter out = new StringWriter();
            model.write(out, "JSON-LD");
            logger.info(out.toString());
            return new ResponseEntity<>(
                    out.toString(),
                    HttpStatus.ACCEPTED
            );
        } catch(Exception e) {
            logger.error(e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static void applyAnonymizationForObject(Resource anonymizationObject, Map<Property, Configuration> configurations, Model model) {
        List<Property> attributes = QueryService.getAttributes(model, configurations.keySet(), anonymizationObject);
        Map<Resource, Map<Property, Literal>> data = QueryService.getData(model, attributes, anonymizationObject);
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
        KpiService.addKpiObject(model, anonymizationObject, attributes, configurations);
        QueryService.deleteOriginalAttributes(model, attributes, anonymizationObject);
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
