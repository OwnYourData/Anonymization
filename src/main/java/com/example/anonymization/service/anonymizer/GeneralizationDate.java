package com.example.anonymization.service.anonymizer;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GeneralizationDate extends Generalization<Date> {

    @Override
    protected List<Pair<Resource, Date>> getSortedValues(Map<Resource, Literal> data) {
        return data.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), getDateValue(e.getValue())))
                .sorted(Comparator.comparing(Pair::getRight))
                .toList();
    }

    private Date getDateValue(Literal o) {
        Object value = o.getValue();

        if (value instanceof java.util.Date) {
            return (java.util.Date) value;
        } else {
            throw new IllegalArgumentException("The value " + value + " is not a Date");
        }
    }
}
