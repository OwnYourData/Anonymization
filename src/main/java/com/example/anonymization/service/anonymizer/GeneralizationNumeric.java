package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GeneralizationNumeric extends Generalization<Double> {

    public GeneralizationNumeric(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject
    ) {
        super(model, property, data, config, anonymizationObject, numberAttributes);
    }

    @Override
    protected List<Pair<Resource, Double>> getSortedValues(Map<Resource, RDFNode> data) {
        try {
            return data.entrySet().stream()
                    .map(e -> new Pair<>(e.getKey(), e.getValue().asLiteral().getDouble()))
                    .sorted(Comparator.comparingDouble(Pair::getRight))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while parsing numeric values for generalization.", e);
        }
    }
}
