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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Service
public class AnonymizationService {

        private static final Logger logger = LoggerFactory.getLogger(AnonymizationService.class);

        public static ResponseEntity<String> applyAnonymization(AnonymizationJsonLDRequestDto request) {
                logger.info("Starting JSON-LD anonymization [configUrl={}, kpi={}, includeOriginal={}, useAdjustedAttrs={}]",
                                request.getConfigurationUrl(), request.isCalculateKpi(),
                                request.isIncludeOriginalData(), request.isUseAdjustedAttributes());
                Map<Resource, Map<Property, Configuration>> anonymizationObjects = ConfigurationService
                                .fetchConfigForObjects(request.getConfigurationUrl());
                Model model = getModel(request.getData());
                anonymizationObjects.forEach(
                                (o, c) -> applyAnonymizationForObject(
                                                o, c, model, request.isCalculateKpi(), request.isIncludeOriginalData(),
                                                request.getRandomSeed(), request.isUseAdjustedAttributes()));
                logger.info("JSON-LD anonymization completed [objectsProcessed={}]", anonymizationObjects.size());
                StringWriter out = new StringWriter();
                model.write(out, "JSON-LD");
                logger.debug("JSON-LD output size: {} characters", out.toString().length());
                return new ResponseEntity<>(
                                out.toString(),
                                HttpStatus.ACCEPTED);
        }

        public static ResponseEntity<String> applyAnonymizationFlatJson(
                        AnonymizationFlatJsonRequestDto request) throws JsonProcessingException {
                logger.info("Starting flat-JSON anonymization [configUrl={}, dataEntries={}, kpi={}, includeOriginal={}]",
                                request.getConfigurationUrl(),
                                request.getData() != null ? request.getData().size() : 0,
                                request.isCalculateKpi(), request.isIncludeOriginalData());
                Model model = ModelFactory.createDefaultModel();
                FaltJsonService.addDataToFlatModel(model, request.getData(), request.getPrefix());
                Map<Resource, Map<Property, Configuration>> anonymizationObjects = ConfigurationService
                                .fetchConfigForObjects(request.getConfigurationUrl());
                anonymizationObjects.forEach(
                                (o, c) -> applyAnonymizationForObject(
                                                o, c, model, request.isCalculateKpi(), request.isIncludeOriginalData(),
                                                request.getRandomSeed(), request.isUseAdjustedAttributes()));
                String out = FaltJsonService.createFlatJsonOutput(
                                model,
                                ConfigurationService.createFlatConfig(anonymizationObjects),
                                anonymizationObjects.keySet(),
                                request.getPrefix(),
                                request.isCalculateKpi());
                logger.info("Flat-JSON anonymization completed [objectsProcessed={}]", anonymizationObjects.size());
                logger.debug("Flat-JSON output size: {} characters", out.length());
                return new ResponseEntity<>(out, HttpStatus.ACCEPTED);
        }

        private static void applyAnonymizationForObject(
                        Resource anonymizationObject,
                        Map<Property, Configuration> configurations,
                        Model model,
                        boolean calculateKpi,
                        boolean includeOriginalData,
                        long seed,
                        boolean useAdjustedAttributes) {
                logger.debug("Applying anonymization for object [uri={}, properties={}, kpi={}, includeOriginal={}]",
                                anonymizationObject.getURI(), configurations.size(), calculateKpi, includeOriginalData);
                Set<Property> attributes = QueryService.getProperties(model, configurations.keySet(),
                                anonymizationObject);
                Map<Resource, Map<Property, RDFNode>> data = QueryService.getData(model, attributes,
                                anonymizationObject);
                logger.debug("Data fetched for anonymization [object={}, attributes={}, records={}]",
                                anonymizationObject.getLocalName(), attributes.size(), data.size());
                Map<Property, Map<Resource, RDFNode>> horizontalData = convertToHorizontalSchema(data, attributes);
                int nrAnonymizeAttributes = getNumberOfAnonymizingAttributes(configurations, attributes);
                horizontalData.entrySet().stream().map(e -> configurations.get(e.getKey()).createAnonymization(
                                model,
                                e.getKey(),
                                e.getValue(),
                                nrAnonymizeAttributes,
                                anonymizationObject,
                                calculateKpi,
                                seed)).forEach(Anonymization::anonymization);
                logger.debug("Anonymization applied for object [uri={}]", anonymizationObject.getURI());
                if (calculateKpi) {
                        KpiService.addKpiObject(model, anonymizationObject, attributes, configurations);
                        logger.debug("KPI added for object [uri={}]", anonymizationObject.getURI());
                }
                if (!includeOriginalData) {
                        QueryService.deleteOriginalProperties(model, attributes, anonymizationObject);
                        logger.debug("Original data removed for object [uri={}]", anonymizationObject.getURI());
                }
                // Rename properties if useAdjustedAttributes=false (AFTER deleting original
                // values)
                if (!useAdjustedAttributes) {
                        renameAnonymizedProperties(model, attributes, configurations);
                        logger.debug("Anonymized properties renamed to original names for object [uri={}]",
                                        anonymizationObject.getURI());
                }
        }

        private static Map<Property, Map<Resource, RDFNode>> convertToHorizontalSchema(
                        Map<Resource, Map<Property, RDFNode>> data,
                        Set<Property> properties) {
                Map<Property, Map<Resource, RDFNode>> propertyMap = new HashMap<>();
                properties.forEach(property -> propertyMap.put(property, new HashMap<>()));
                data.forEach((resource, value) -> value.forEach(
                                (property, node) -> propertyMap.get(property).put(resource, node)));
                return propertyMap;
        }

        private static int getNumberOfAnonymizingAttributes(
                        Map<Property, Configuration> configs,
                        Set<Property> attributes) {
                return (int) attributes.stream()
                                .map(configs::get)
                                .map(Configuration::getAnonymization)
                                .filter(anonymization -> List.of("generalization", "randomization")
                                                .contains(anonymization))
                                .count();
        }

        private static Model getModel(JsonNode data) {
                try {
                        String jsonLdString = data.toString();
                        Model model = ModelFactory.createDefaultModel();
                        RDFDataMgr.read(model, new StringReader(jsonLdString), null, RDFLanguages.JSONLD);
                        return model;
                } catch (Exception e) {
                        throw new RequestModelException(
                                        "The Request Data could not be converted ot a model: " + e.getMessage());
                }
        }

        /**
         * Renames anonymized properties to original property names when
         * useAdjustedAttributes=false.
         * For each property, finds the corresponding suffixed property (_randomized,
         * _generalized, _masked),
         * copies its values to the original property name, and removes the suffixed
         * property.
         */
        private static void renameAnonymizedProperties(
                        Model model,
                        Set<Property> attributes,
                        Map<Property, Configuration> configurations) {
                for (Property originalProperty : attributes) {
                        Configuration config = configurations.get(originalProperty);
                        if (config == null) {
                                continue;
                        }

                        String suffix = switch (config.getAnonymization()) {
                                case "randomization" -> "_randomized";
                                case "generalization" -> "_generalized";
                                case "masking" -> "_masked";
                                default -> null;
                        };

                        if (suffix == null)
                                continue;

                        Property suffixedProperty = model.createProperty(originalProperty.getURI() + suffix);

                        // Copy all statements from suffixed property to original property
                        List<Statement> statementsToAdd = new ArrayList<>();
                        StmtIterator iter = model.listStatements(null, suffixedProperty, (RDFNode) null);
                        while (iter.hasNext()) {
                                Statement stmt = iter.next();
                                statementsToAdd.add(model.createStatement(stmt.getSubject(), originalProperty,
                                                stmt.getObject()));
                        }

                        // Remove original property values first
                        model.removeAll(null, originalProperty, null);

                        // Add copied values to original property
                        for (Statement stmt : statementsToAdd) {
                                model.add(stmt);
                        }

                        // Remove the suffixed property
                        model.removeAll(null, suffixedProperty, null);
                }
        }
}
