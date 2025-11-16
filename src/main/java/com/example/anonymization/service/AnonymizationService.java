package com.example.anonymization.service;

import com.example.anonymization.dto.AnonymizationFlatJsonRequestDto;
import com.example.anonymization.dto.AnonymizationJsonLDRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.exceptions.RequestModelException;
import com.example.anonymization.service.anonymizer.Anonymization;
import com.example.anonymization.data.QueryService;
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
    public static final String FLAT_OBJECT_NAME = "anonymizationObject";

    public static ResponseEntity<String> applyAnonymization(AnonymizationJsonLDRequestDto request) {
        Map<Resource, Map<Property, Configuration>> anonymizationObjects =
                ConfigurationService.fetchConfigForObjects(request.getConfigurationUrl());
        Model model = getModel(request.getData());
        anonymizationObjects.forEach(
                (o, c) -> applyAnonymizationForObject(o, c, model)
        );
        StringWriter out = new StringWriter();
        model.write(out, "JSON-LD");
        logger.info(out.toString());
        return new ResponseEntity<>(
                out.toString(),
                HttpStatus.ACCEPTED
        );
    }
    // TODO include the original type to also find objects without a type set --> combination with new type implementation
    public static ResponseEntity<String> applyAnonymizationFlatJson(AnonymizationFlatJsonRequestDto request) {
        Map<Property, Configuration> configs = ConfigurationService.fetchFlatConfig(request.getConfigurationUrl());
        Model model = ModelFactory.createDefaultModel();
        FaltJsonService.addDataToFlatModel(model, request.getData(), request.getPrefix());
        Map<Resource, Map<Property, Configuration>> anonymizationObjects =
                ConfigurationService.fetchConfigForObjects(request.getConfigurationUrl());
        anonymizationObjects.forEach(
                (o, c) -> applyAnonymizationForObject(o, c, model)
        );
        String out = FaltJsonService.createFlatJsonOutput(model, anonymizationObjects.keySet(), configs);
        logger.info(out);
        return new ResponseEntity<>(out, HttpStatus.ACCEPTED);
    }

    private static void applyAnonymizationForObject(
            Resource anonymizationObject,
            Map<Property, Configuration> configurations,
            Model model
    ) {
        Set<Property> attributes = QueryService.getProperties(model, configurations.keySet(), anonymizationObject);
        Map<Resource, Map<Property, Literal>> data = QueryService.getData(model, attributes, Set.of(anonymizationObject));
        Map<Property, Map<Resource, Literal>> horizontalData = convertToHorizontalSchema(data, attributes);
        int nrAnonymizeAttributes = getNumberOfAnonymizingAttributes(configurations, attributes);
        horizontalData.entrySet().stream().map(e -> Anonymization.anonymizationFactoryFunction(
                configurations.get(e.getKey()),
                model,
                e.getKey(),
                e.getValue(),
                nrAnonymizeAttributes,
                anonymizationObject
        )).forEach(Anonymization::anonymization);
        KpiService.addKpiObject(model, anonymizationObject, attributes, configurations);
        QueryService.deleteOriginalProperties(model, attributes, anonymizationObject);
    }

    private static Map<Property, Map<Resource, Literal>> convertToHorizontalSchema(
            Map<Resource, Map<Property, Literal>> data,
            Set<Property> properties
    ) {
        Map<Property, Map<Resource, Literal>> propertyMap = new HashMap<>();
        properties.forEach(property -> propertyMap.put(property, new HashMap<>()));
        data.forEach((resource, value) -> value.forEach(
                (property, literal) -> propertyMap.get(property).put(resource, literal)
        ));
        return propertyMap;
    }

    private static int getNumberOfAnonymizingAttributes(
            Map<Property, Configuration> configs,
            Set<Property> attributes
    ) {
        return (int) attributes.stream()
                .map(configs::get)
                .map(Configuration::getAnonymization)
                .filter(anonymization -> List.of("generalization", "randomization").contains(anonymization))
                .count();
    }

    private static Model getModel(JsonNode data) {
        try {
            String jsonLdString = data.toString();
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, new StringReader(jsonLdString), null, RDFLanguages.JSONLD);
            return model;
        } catch (Exception e) {
            throw new RequestModelException("The Request Data coudl not be converted ot a model: " + e.getMessage());
        }
    }
}
