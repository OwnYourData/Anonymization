package com.example.anonymization.service;

import com.example.anonymization.dto.AnonymizationFlatJsonRequestDto;
import com.example.anonymization.dto.AnonymizationJsonLDRequestDto;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.exceptions.RequestModelException;
import com.example.anonymization.service.anonymizer.Anonymization;
import com.example.anonymization.data.QueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    public static ResponseEntity<String> applyAnonymization(AnonymizationJsonLDRequestDto request) {
        logger.info("Starting json-ld anonymization process");
        Map<Resource, Map<Property, Configuration>> anonymizationObjects =
                ConfigurationService.fetchConfigForObjects(request.getConfigurationUrl());
        Model model = getModel(request.getData());
        anonymizationObjects.forEach(
                (o, c) -> applyAnonymizationForObject(
                        o, c, model, request.isCalculateKpi(), request.isIncludeOriginalData(), request.getRandomSeed()
                )
        );
        logger.info("Finished json-ld anonymization process");
        StringWriter out = new StringWriter();
        model.write(out, "JSON-LD");
        logger.info(out.toString());
        return new ResponseEntity<>(
                out.toString(),
                HttpStatus.ACCEPTED
        );
    }

    public static ResponseEntity<String> applyAnonymizationFlatJson(
            AnonymizationFlatJsonRequestDto request
    ) throws JsonProcessingException {
        logger.info("Starting flat-json anonymization process");
        Model model = ModelFactory.createDefaultModel();
        FaltJsonService.addDataToFlatModel(model, request.getData(), request.getPrefix());
        Map<Resource, Map<Property, Configuration>> anonymizationObjects =
                ConfigurationService.fetchConfigForObjects(request.getConfigurationUrl());
        anonymizationObjects.forEach(
                (o, c) -> applyAnonymizationForObject(
                        o, c, model, request.isCalculateKpi(), request.isIncludeOriginalData(), request.getRandomSeed()
                )
        );
        String out = FaltJsonService.createFlatJsonOutput(
                model,
                ConfigurationService.createFlatConfig(anonymizationObjects),
                anonymizationObjects.keySet(),
                request.getPrefix(),
                request.isCalculateKpi()
        );
        logger.info("Finished flat-json anonymization process");
        return new ResponseEntity<>(out, HttpStatus.ACCEPTED);
    }

    private static void applyAnonymizationForObject(
            Resource anonymizationObject,
            Map<Property, Configuration> configurations,
            Model model,
            boolean calculateKpi,
            boolean includeOriginalData,
            long seed
    ) {
        logger.info("Applying anonymization for object: {}", anonymizationObject.getURI());
        Set<Property> attributes = QueryService.getProperties(model, configurations.keySet(), anonymizationObject);
        Map<Resource, Map<Property, RDFNode>> data = QueryService.getData(model, attributes, anonymizationObject);
        Map<Property, Map<Resource, RDFNode>> horizontalData = convertToHorizontalSchema(data, attributes);
        int nrAnonymizeAttributes = getNumberOfAnonymizingAttributes(configurations, attributes);
        horizontalData.entrySet().stream().map(e ->
                configurations.get(e.getKey()).createAnonymization(
                        model,
                        e.getKey(),
                        e.getValue(),
                        nrAnonymizeAttributes,
                        anonymizationObject,
                        calculateKpi,
                        seed
        )).forEach(Anonymization::anonymization);
        logger.info("Anonymization applied for object: {}", anonymizationObject.getURI());
        if (calculateKpi) {
            KpiService.addKpiObject(model, anonymizationObject, attributes, configurations);
            logger.info("Kpi added for object: {}", anonymizationObject.getURI());
        }
        if (!includeOriginalData) {
            QueryService.deleteOriginalProperties(model, attributes, anonymizationObject);
            logger.info("Original data removed for object: {}", anonymizationObject.getURI());
        }
    }

    private static Map<Property, Map<Resource, RDFNode>> convertToHorizontalSchema(
            Map<Resource, Map<Property, RDFNode>> data,
            Set<Property> properties
    ) {
        Map<Property, Map<Resource, RDFNode>> propertyMap = new HashMap<>();
        properties.forEach(property -> propertyMap.put(property, new HashMap<>()));
        data.forEach((resource, value) -> value.forEach(
                (property, node) -> propertyMap.get(property).put(resource, node)
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
            throw new RequestModelException("The Request Data could not be converted ot a model: " + e.getMessage());
        }
    }
}
