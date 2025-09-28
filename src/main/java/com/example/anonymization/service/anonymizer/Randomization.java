package com.example.anonymization.service.anonymizer;

import com.example.anonymization.service.KpiService;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.*;

public abstract class Randomization implements Anonymization {

    abstract double distance(Literal a, Literal b);

    abstract Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max);

    abstract Comparator<Literal> getComparator();

    @Override
    public void applyAnonymization(Model model, Property property, Map<Resource, Literal> data, long numberAttributes) {
        int nrBuckets = Anonymization.calculateNumberOfBuckets(data.size(), numberAttributes);
        int randomizationValue = data.size() / nrBuckets;
        KpiService.addNrBuckets(model, property, nrBuckets);
        Map<Resource, Literal> randomizedValues = getRandomizedValues(data, randomizationValue);
        writeToModel(model, randomizedValues, property);
    }

    private Map<Resource, Literal> getRandomizedValues(Map<Resource, Literal> data, int randomizationValue) {
        List<Map.Entry<Resource, Literal>> sorted = data.entrySet().stream()
                .sorted((e1, e2) -> getComparator().compare(e1.getValue(), e2.getValue()))
                .toList();
        Map<Resource, Literal> randomized = HashMap.newHashMap(data.size());
        randomizationValue = randomizationValue == data.size() ? randomizationValue - 1 : randomizationValue;
        int lowerBound = 0;
        for (int idx = 0; idx < sorted.size(); idx++) {
            Map.Entry<Resource, Literal> entry = sorted.get(idx);

            while (
                    lowerBound < sorted.size() - (randomizationValue + 1) &&
                            (lowerBound < idx - randomizationValue ||
                                    Math.abs(distance(sorted.get(lowerBound).getValue(), entry.getValue())) >
                                            Math.abs(distance(sorted.get(lowerBound + randomizationValue + 1).getValue(), entry.getValue()))
                            )
            ) {
                lowerBound++;
            }

            double dist = Math.max(
                    Math.abs(distance(sorted.get(lowerBound).getValue(), entry.getValue())),
                    Math.abs(distance(sorted.get(lowerBound + randomizationValue).getValue(), entry.getValue()))
            );
            randomized.put(
                    entry.getKey(),
                    createRandomizedLiteral(
                            entry.getValue(), dist, sorted.getFirst().getValue(), sorted.getLast().getValue()
                    )
            );
        }
        return randomized;
    }

    private void writeToModel(Model model, Map<Resource, Literal> randomizedValues, Property originalProperty) {
        Property randomized = model.createProperty(originalProperty.getURI(), "_randomized");
        randomizedValues.forEach((key, value) -> key.addLiteral(randomized, value));
    }
}
