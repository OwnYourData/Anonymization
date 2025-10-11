package com.example.anonymization.service;

import com.example.anonymization.data.QueryService;
import com.example.anonymization.entities.Configuration;
import org.apache.jena.atlas.lib.Pair;
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

    static String createFlatJsonOutput(Model model, Resource objectType, Map<Property, Configuration> configs) {
        Map<Resource, Map<Property, Literal>> data = QueryService.getAllData(model, objectType);
        Set<Property> classificationProperties = configs.entrySet().stream()
                .filter(e -> "classification".equals(e.getValue().getAnonymization()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Map<Resource, Map<Property, List<Literal>>> generalizatoinData = QueryService.getAllRandomizationData(model, objectType, classificationProperties);
        return "";
    }
}
