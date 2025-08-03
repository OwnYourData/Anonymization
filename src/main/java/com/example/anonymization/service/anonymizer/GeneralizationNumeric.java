package com.example.anonymization.service.anonymizer;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.List;
import java.util.Map;

public class GeneralizationNumeric extends Generalization<Double> {

    @Override
    public void applyAnoynmization(Model model, Property property, Map<Resource, Literal> data) {
        List<Pair<Resource, Double>> sortedValues = data.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), e.getValue().getDouble()))
                .sorted((e1, e2) -> (int) (e1.getRight() - e2.getRight()))
                .toList();

        Double[] sortedArray = sortedValues.stream().map(Pair::getRight).toArray(Double[]::new);
        Map<Resource, Double[]> ranges = getRanges(sortedValues, sortedArray);
        writeToModel(model, ranges, property);
    }

    @Override
    protected Double[] getBucketRange(Double[] sortedValues, int bucketNumber) {
        Double[] minMax = new Double[2];
        minMax[0] = sortedValues[bucketNumber * 3];
        minMax[1] = sortedValues[((bucketNumber + 1) * 3) - 1];
        return minMax;
    }
}
