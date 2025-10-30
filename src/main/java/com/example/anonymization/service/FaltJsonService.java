package com.example.anonymization.service;

import com.example.anonymization.data.QueryService;
import com.example.anonymization.entities.Configuration;
import com.example.anonymization.exceptions.AnonymizationException;
import com.example.anonymization.exceptions.RequestModelException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FaltJsonService {

    /**
     * Adds data to a flat model with the given object type and prefix.
     * @param model model to which the data is added
     * @param objectType type of the anonymization object
     * @param data data to be added
     * @param prefix prefix for the properties
     */
    public static void addDataToFlatModel(Model model, Resource objectType, List<Map<String, Object>> data, String prefix) {
        int counter = 0;
        for (Map<String, Object> entry : data) {
            try {
                Resource object = model.createResource(prefix + "object" + counter);
                object.addProperty(RDF.type, objectType);

                // Add counter property
                Property counterProperty = model.createProperty(prefix, "counter");
                object.addLiteral(counterProperty, counter);

                for (Map.Entry<String, Object> kv : entry.entrySet()) {
                    String key = kv.getKey();
                    validateKey(key);
                    Object value = kv.getValue();
                    Property property = model.createProperty(prefix, key);

                    if (value != null) {
                        object.addProperty(property, value.toString());
                    }
                }
            } catch (Exception ex) {
                throw new RequestModelException("Error adding data to flat model: " + ex.getMessage());
            }
            counter++;
        }
    }

    /**
     * Creates a flat JSON output from the given model, object type, and configurations.
     * @param model model containing the data
     * @param objectType type of the anonymization object
     * @param configs configurations for the properties
     * @return String representing the flat JSON output in Json format
     */
    public static String createFlatJsonOutput(Model model, Resource objectType, Map<Property, Configuration> configs) {
        Map<Resource, Map<Property, Literal>> data = QueryService.getAllData(model, objectType);
        Set<Property> classificationProperties = configs.entrySet().stream()
                .filter(e -> "generalization".equals(e.getValue().getAnonymization()))
                .map(Map.Entry::getKey)
                .map(p -> model.getProperty(p.getURI() + "_generalized"))
                .collect(Collectors.toSet());
        Map<Resource, Map<Property, List<Literal>>> generalizationData =
                QueryService.getGeneralizationData(model, objectType, classificationProperties);
        Map<Property, Literal> kpiData = QueryService.getKpiData(model);
        return createFlatJsonString(data, generalizationData , kpiData);
    }

    private static String createFlatJsonString(
            Map<Resource, Map<Property, Literal>> data,
            Map<Resource, Map<Property, List<Literal>>> generalizationData,
            Map<Property, Literal> kpiData
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode dataArray = addDataToArrayNode(mapper, data, generalizationData);
            ObjectNode kpiNode = addKpisToObjectNode(mapper, kpiData);

            ObjectNode root = mapper.createObjectNode();
            root.set("data", dataArray);
            root.set("kpis", kpiNode);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new AnonymizationException("Error creating flat JSON output: " + e.getMessage());
        }
    }

    private static ArrayNode addDataToArrayNode(
            ObjectMapper mapper,
            Map<Resource, Map<Property, Literal>> data,
            Map<Resource, Map<Property, List<Literal>>> generalizationData
    ) {
        ArrayNode dataArray = mapper.createArrayNode();
        List<Resource> sortedResources = data.keySet().stream()
                .sorted((r1, r2) -> {
                    int c1 = getCounterValue(data.get(r1));
                    int c2 = getCounterValue(data.get(r2));
                    return Integer.compare(c1, c2);
                })
                .toList();

        for (Resource resource : sortedResources) {
            ObjectNode entryNode = mapper.createObjectNode();

            Map<Property, Literal> attrs = data.get(resource);
            if (attrs != null) {
                for (Map.Entry<Property, Literal> attr : attrs.entrySet()) {
                    if (!"counter".equals(attr.getKey().getLocalName())) {
                        entryNode.put(attr.getKey().getLocalName(), attr.getValue().getValue().toString());
                    }
                }
            }

            Map<Property, List<Literal>> genAttrs = generalizationData.get(resource);
            if (genAttrs != null) {
                for (Map.Entry<Property, List<Literal>> genAttr : genAttrs.entrySet()) {
                    List<Literal> values = genAttr.getValue();
                    if (values.size() == 2) {
                        ObjectNode genNode = mapper.createObjectNode();
                        genNode.put("min", values.get(0).getValue().toString());
                        genNode.put("max", values.get(1).getValue().toString());
                        entryNode.set(genAttr.getKey().getLocalName(), genNode);
                    }
                }
            }

            dataArray.add(entryNode);
        }
        return dataArray;
    }

    private static ObjectNode addKpisToObjectNode(
            ObjectMapper mapper,
            Map<Property, Literal> kpiData
    ) {
        ObjectNode kpiNode = mapper.createObjectNode();
        kpiData.forEach((kpi, value) -> {
            kpiNode.put(kpi.getLocalName(), value.getValue().toString());
        });
        return kpiNode;
    }


    private static int getCounterValue(Map<Property, Literal> attrs) {
        if (attrs == null) return Integer.MAX_VALUE;
        for (Map.Entry<Property, Literal> attr : attrs.entrySet()) {
            if ("counter".equals(attr.getKey().getLocalName())) {
                Object val = attr.getValue().getValue();
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                }
                try {
                    return Integer.parseInt(val.toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return Integer.MAX_VALUE;
    }

    private static void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new RequestModelException("Property key cannot be null or empty");
        }
        if (!key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new RequestModelException("Invalid property key: " + key +
                    ". It must start with a letter or underscore and contain only letters, digits, or underscores.");
        }
    }

}
