package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GeneralizationDate extends Generalization<LocalDate> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public GeneralizationDate(
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
    protected List<Pair<Resource, LocalDate>> getSortedValues(Map<Resource, RDFNode> data) {
        return data.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), toDate(e.getValue())))
                .sorted(Comparator.comparing(Pair::getRight))
                .toList();
    }

    @Override
    protected LocalDate getMedianValue(LocalDate value1, LocalDate value2) {
        if (value1 == null) {
            return value2;
        }
        if (value2 == null) {
            return value1;
        }
        return LocalDate.ofEpochDay((value1.toEpochDay() + value2.toEpochDay()) / 2);
    }

    private static LocalDate toDate(RDFNode node) {
        try {
            return LocalDate.parse(node.asLiteral().getLexicalForm(), DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Literal lexical form is not a valid xsd:date: " + node, e);
        }
    }
}
