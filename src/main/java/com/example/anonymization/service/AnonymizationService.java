package com.example.anonymization.service;

import com.example.anonymization.dto.AnonymizationRequestDto;
import org.apache.jena.rdf.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class AnonymizationService {

    public static ResponseEntity<String> applyAnonymization(AnonymizationRequestDto request) {

        Model model = ModelFactory.createDefaultModel();
        String pathToFile = "input_twoargument.ttl"; // TODO: extract from query send in json-ld


        try (InputStream in = new FileInputStream(pathToFile)) {
            model.read(in, null, "TTL");
            model.write(System.out, "TTL");
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Property> attributes = List.of(model.createProperty("attribute"), model.createProperty("attribute2")); // TODO: extract from model
        Map<Resource, Map<Property, Literal>> data = OntologyService.extractDataFromModel(model, attributes, "testobject");
        OntologyService.deleteOldValues(model, attributes, "testobject");
        model.write(System.out, "TTL");

        /*Property min = anonymizedModel.createProperty(NS, "min");
        Property max = anonymizedModel.createProperty(NS, "max");

        anonymized.forEach((key, value) -> {
            Resource res = anonymizedModel.createResource(key);
            res.addLiteral(min, value[0]);
            res.addLiteral(max, value[1]);
        });*/

        return new ResponseEntity<>(
                "Response",
                HttpStatus.ACCEPTED
        );
    }

    private static Map<String, Double[]> generalization(Map<String, Integer> data) {
        List<Map.Entry<String, Integer>> sortedValues = data.entrySet()
               .stream().sorted((e1, e2) -> e1.getValue() - e2.getValue()).toList();

        double[] sortedArray = sortedValues.stream().mapToDouble(Map.Entry::getValue).toArray();
        List<AbstractMap.SimpleEntry<String, Integer>> positionValues = new LinkedList<>();
        for (int i = 0; i < sortedValues.size(); i++) {
            positionValues.add(new AbstractMap.SimpleEntry<>(
                    sortedValues.get(i).getKey(),
                    3 * i / data.size())
            );
        }
        return positionValues.stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), getBucketRange(sortedArray, e.getValue())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private static Double[] getBucketRange(double[] sortedValues, int bucket) {
        Double[] minMax = new Double[2];
        minMax[0] = sortedValues[bucket * 3];
        minMax[1] = sortedValues[((bucket + 1) * 3) - 1];
        return minMax;
    }
}
