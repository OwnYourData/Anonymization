package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.*;

import java.util.*;

public abstract class Randomization extends Anonymization<Configuration> {

    public Randomization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            Configuration config,
            Resource anonymizationObject,
            long numberAttributes,
            boolean calculateKpi) {
        super(model, property, data, config, anonymizationObject, numberAttributes, calculateKpi);
    }

    abstract double distance(Literal a, Literal b);

    abstract Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max);

    abstract Comparator<Literal> getComparator();

    @Override
    public void applyAnonymization() {
        int randomizationValue = data.size() / numberBuckets;
        Map<Resource, Literal> randomizedValues = getRandomizedValues(data, randomizationValue);
        writeToModel(model, randomizedValues, property);
    }

    private Map<Resource, Literal> getRandomizedValues(Map<Resource, RDFNode> data, int randomizationValue) {
        List<Map.Entry<Resource, Literal>> sorted = getSortedLiteralEntries(data);
        Map<Resource, Literal> randomized = HashMap.newHashMap(data.size());
        randomizationValue = randomizationValue == data.size() ? randomizationValue - 1 : randomizationValue;
        int lowerBound = 0;
        for (int idx = 0; idx < sorted.size(); idx++) {
            Map.Entry<Resource, Literal> entry = sorted.get(idx);

            while (lowerBound < sorted.size() - (randomizationValue + 1) &&
                    (lowerBound < idx - randomizationValue ||
                            Math.abs(distance(sorted.get(lowerBound).getValue(), entry.getValue())) > Math.abs(distance(
                                    sorted.get(lowerBound + randomizationValue + 1).getValue(),
                                    entry.getValue())))) {
                lowerBound++;
            }

            double dist = Math.max(
                    Math.abs(distance(sorted.get(lowerBound).getValue(), entry.getValue())),
                    Math.abs(distance(sorted.get(lowerBound + randomizationValue).getValue(), entry.getValue())));
            randomized.put(
                    entry.getKey(),
                    createRandomizedLiteral(
                            entry.getValue(), dist, sorted.getFirst().getValue(), sorted.getLast().getValue()));
        }
        return randomized;
    }

    private List<Map.Entry<Resource, Literal>> getSortedLiteralEntries(Map<Resource, RDFNode> data) {
        try {
            return data.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().asLiteral()))
                    .sorted((e1, e2) -> getComparator().compare(e1.getValue(), e2.getValue()))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("Randomization can only be applied to literal values.");
        }
    }

    private void writeToModel(Model model, Map<Resource, Literal> randomizedValues, Property originalProperty) {
        Property randomized = model.createProperty(originalProperty.getURI(), "_randomized");
        randomizedValues.forEach((key, value) -> key.addLiteral(randomized, value));
    }
}
