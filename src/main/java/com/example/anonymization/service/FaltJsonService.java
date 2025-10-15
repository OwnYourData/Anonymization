package com.example.anonymization.service;

import com.example.anonymization.data.QueryService;
import com.example.anonymization.entities.Configuration;
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

    static void addDataToFlatModel(Model model, Resource objectType, List<Map<String, Object>> data, String prefix) {
        int counter = 0;
        for (Map<String, Object> entry : data) {
            Resource object = model.createResource(prefix + "object" + counter);
            object.addProperty(RDF.type, objectType);

            // Add counter property
            Property counterProperty = model.createProperty(prefix, "counter");
            object.addLiteral(counterProperty, counter);

            for (Map.Entry<String, Object> kv : entry.entrySet()) {
                String key = kv.getKey();
                Object value = kv.getValue();
                Property property = model.createProperty(prefix, key);

                if (value != null) {
                    object.addProperty(property, value.toString());
                }
            }
            counter++;
        }
    }

    static String createFlatJsonOutput(
            Model model,
            Resource objectType, Map<Property, Configuration> configs
    ) throws JsonProcessingException {
        Map<Resource, Map<Property, Literal>> data = QueryService.getAllData(model, objectType);
        Set<Property> classificationProperties = configs.entrySet().stream()
                .filter(e -> "generalization".equals(e.getValue().getAnonymization()))
                .map(Map.Entry::getKey)
                .map(p -> model.getProperty(p.getURI() + "_generalized"))
                .collect(Collectors.toSet());
        Map<Resource, Map<Property, List<Literal>>> generalizatoinData =
                QueryService.getGeneralizationData(model, objectType, classificationProperties);
        Map<Property, Literal> kpiData = QueryService.getKpiData(model);
        return createFlatJsonString(data, generalizatoinData , kpiData);
    }

    private static String createFlatJsonString(
            Map<Resource, Map<Property, Literal>> data,
            Map<Resource, Map<Property, List<Literal>>> generalizationData,
            Map<Property, Literal> kpiData
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode dataArray = addDataToArrayNode(mapper, data, generalizationData);
        ObjectNode kpiNode = addKpisToObjectNode(mapper, kpiData);

        ObjectNode root = mapper.createObjectNode();
        root.set("data", dataArray);
        root.set("kpis", kpiNode);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
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

}
