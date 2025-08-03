package com.example.anonymization.service.anonymizer;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import java.util.List;
import java.util.Map;

public class GeneralizationNumeric extends Generalization<Double> {

    @Override
    protected List<Pair<Resource, Double>> getSortedValues(Map<Resource, Literal> data) {
        return data.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), e.getValue().getDouble()))
                .sorted((e1, e2) -> (int) (e1.getRight() - e2.getRight()))
                .toList();
    }
}
