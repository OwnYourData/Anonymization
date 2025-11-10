package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.List;
import java.util.Map;

public class GeneralizationNumeric extends Generalization<Double> {

    public GeneralizationNumeric(
            Model model,
            Property property,
            Map<Resource, Literal> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject
    ) {
        super(model, property, data, config, anonymizationObject, numberAttributes);
    }

    @Override
    protected List<Pair<Resource, Double>> getSortedValues(Map<Resource, Literal> data) {
        return data.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), e.getValue().getDouble()))
                .sorted((e1, e2) -> (int) (e1.getRight() - e2.getRight()))
                .toList();
    }
}
