package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class RandomizationNumeric extends Randomization {

    public RandomizationNumeric(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject,
            boolean calculateKpi
    ) {
        super(model, property, data, config, anonymizationObject, numberAttributes, calculateKpi);
    }

    @Override
    protected double distance(Literal a, Literal b) {
        return a.getDouble() - b.getDouble();
    }

    @Override
    protected Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max) {
        double noise;
        double randomizedValue = Double.MAX_VALUE;
        while(randomizedValue > max.getDouble() || randomizedValue < min.getDouble()) {
            noise = new Random().nextDouble() * distance;
            randomizedValue = value.getDouble() + noise > max.getDouble() ||
                    value.getDouble() + noise < min.getDouble() ?
                    value.getDouble() - noise : value.getDouble() + noise;
        }
        return ResourceFactory.createTypedLiteral(randomizedValue);
    }

    @Override
    Comparator<Literal> getComparator() {
        return Comparator.comparingDouble(Literal::getDouble);
    }
}
