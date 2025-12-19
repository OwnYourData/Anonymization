package com.example.anonymization.service;

import com.example.anonymization.data.QueryService;
import com.example.anonymization.entities.Configuration;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class FaltJsonService {

    public static final String FLAT_OBJECT_NAME = "anonymizationObject";

    /**
     * Adds data to a flat model with the given object type and prefix.
     * @param model model to which the data is added
     * @param data data to be added
     * @param prefix prefix for the properties
     */
    public static void addDataToFlatModel(Model model, List<Map<String, Object>> data, String prefix) {
        int counter = 0;
        Resource flatObject = model.createResource(prefix + FLAT_OBJECT_NAME);
        for (Map<String, Object> entry : data) {
            Resource object = model.createResource(prefix + "object" + counter);
            object.addProperty(RDF.type, flatObject);

            // Add counter property
            Property counterProperty = model.createProperty(prefix, "counter");
            object.addLiteral(counterProperty, counter);

            for (Map.Entry<String, Object> kv : entry.entrySet()) {
                String key = kv.getKey();
                validateKey(key);
                Object value = kv.getValue();
                if (key.equals("type")) {
                    addTypeProperty(value, object, model, prefix);
                } else {
                    setDataAttribute(object, model, prefix,key, value);
                }
            }
            counter++;
        }
    }

    private static void addTypeProperty(Object value, Resource object, Model model, String prefix) {
        if (value instanceof List<?>) {
            for (Object v : (List<?>) value) {
                object.addProperty(RDF.type, model.createResource(prefix + v.toString()));
            }
        } else if (value != null) {
            object.addProperty(RDF.type, model.createResource(prefix + value));
        }
    }

    private static void setDataAttribute(
            Resource object,
            Model model,
            String prefix,
            String key,
            Object value
    ) {
        if (value instanceof Map<?, ?>) {
            Resource dataObject = model.createResource();
            object.addProperty(model.createProperty(prefix, key), dataObject);
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                dataObject.addProperty(model.createProperty(prefix, entry.getKey().toString()), entry.getValue().toString());
            }
        } else if (value != null) {
            object.addProperty(model.createProperty(prefix, key), value.toString());
        }
    }

    /**
     * Creates a flat JSON output from the given model, object type, and configurations.
     * @param model model containing the data
     * @param configs configurations for the properties
     * @return String representing the flat JSON output in Json format
     */
    public static String createFlatJsonOutput(
            Model model,
            Map<Property, Configuration> configs,
            Collection<Resource> objectTypes,
            String prefix,
            boolean calculateKpi
    ) throws JsonProcessingException {
        Resource flatObject = model.createResource(prefix + FLAT_OBJECT_NAME);
        Map<Resource, Map<Property, Literal>> data = getLiteralData(model, flatObject);
        Map<Resource, List<Resource>> types = QueryService.getTypesForResources(model, flatObject);

        Set<Property> classificationProperties = configs.entrySet().stream()
                .filter(e -> "generalization".equals(e.getValue().getAnonymization()) &&
                        List.of("integer", "float", "date").contains(e.getValue().getDataType()))
                .map(Map.Entry::getKey)
                .map(p -> model.getProperty(p.getURI() + "_generalized"))
                .collect(Collectors.toSet());
        Map<Resource, Map<Property, Literal[]>> generalizationData =
                QueryService.getGeneralizationData(model, flatObject, classificationProperties);
        // TODO remove kpi object if not calculateKpi
        Map<Resource, Long> kAnonymity = QueryService.getKAnonymity(model, objectTypes);
        Map<Resource, List<QueryService.AttributeInformation>> attributeInformation =
                QueryService.getAttributeInformation(model, objectTypes);
        return createFlatJsonString(data, types, generalizationData , kAnonymity, attributeInformation);
    }

    private static Map<Resource, Map<Property, Literal>> getLiteralData(
            Model model,
            Resource flatObject
    ) {
        return QueryService.getAllData(model, flatObject).entrySet().stream()
                        .map(e -> Map.entry(
                                e.getKey(),
                                e.getValue().entrySet().stream()
                                        .filter(inner -> inner.getValue().isLiteral())
                                        .collect(toMap(
                                                Map.Entry::getKey,
                                                inner -> inner.getValue().asLiteral()
                                        ))
                        ))
                        .filter(e -> !e.getValue().isEmpty())
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static String createFlatJsonString(
            Map<Resource, Map<Property, Literal>> data,
            Map<Resource, List<Resource>> types,
            Map<Resource, Map<Property, Literal[]>> generalizationData,
            Map<Resource, Long> kAnonymity,
            Map<Resource, List<QueryService.AttributeInformation>> attributeInformation
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode dataArray = addDataToArrayNode(mapper, data, types, generalizationData);
        ObjectNode kpiNode = addKpisToObjectNode(mapper, kAnonymity, attributeInformation);

        ObjectNode root = mapper.createObjectNode();
        root.set("data", dataArray);
        root.set("kpis", kpiNode);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static ArrayNode addDataToArrayNode(
            ObjectMapper mapper,
            Map<Resource, Map<Property, Literal>> data,
            Map<Resource, List<Resource>> types,
            Map<Resource, Map<Property, Literal[]>> generalizationData
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

            List<Resource> resourceTypes = types.get(resource);
            if (resourceTypes != null && !resourceTypes.isEmpty()) {
                ArrayNode typesArray = mapper.createArrayNode();
                for (Resource type : resourceTypes) {
                    typesArray.add(type.getLocalName());
                }
                entryNode.set("types", typesArray);
            }

            Map<Property, Literal[]> genAttrs = generalizationData.get(resource);
            if (genAttrs != null) {
                for (Map.Entry<Property, Literal[]> genAttr : genAttrs.entrySet()) {
                    Literal[] values = genAttr.getValue();
                    if (values.length == 2) {
                        ObjectNode genNode = mapper.createObjectNode();
                        if (values[0] == null) {
                            genNode.put("min", "obfuscated");
                        } else {
                            genNode.put("min", values[0].getValue().toString());
                        }
                        if (values[1] == null) {
                            genNode.put("max", "obfuscated");
                        } else {
                            genNode.put("max", values[1].getValue().toString());
                        }
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
            Map<Resource, Long> kAnonymity,
            Map<Resource, List<QueryService.AttributeInformation>> attributeInformation
    ) {
        ObjectNode kpiNode = mapper.createObjectNode();
        kAnonymity.forEach((res, kAnonymityValue) -> {
            ObjectNode objNode = mapper.createObjectNode();
            objNode.put("k-Anonymity", kAnonymityValue);
            List<QueryService.AttributeInformation> attrs = attributeInformation.get(res);
            if (attrs != null) {
                attrs.forEach(attr -> {
                    ObjectNode attrNode = mapper.createObjectNode();
                    attrNode.put("anonymization", attr.anonymization());
                    if (attr.nrBuckets() != null) {
                        attrNode.put("nrBuckets",  attr.nrBuckets());
                    }
                    objNode.set(attr.attribute().getLocalName(), attrNode);
                });
            }
            kpiNode.set(res.getLocalName(), objNode);
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
