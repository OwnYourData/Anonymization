package com.example.anonymization.service.anonymizer;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public class GeneralizationNumeric implements Anonymization{

    @Override
    public void applyAnoynmization(Model model, Property property, Map<Resource, Literal> data) {

    }

    /*
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
     */
}
