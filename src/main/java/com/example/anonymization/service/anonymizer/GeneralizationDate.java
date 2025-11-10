package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.*;

public class GeneralizationDate extends Generalization<Calendar> {

    public GeneralizationDate(Model model, Property property, Map<Resource, Literal> data, long numberAttributes, Configuration config, Resource anonymizationObject) {
        super(model, property, data, config, anonymizationObject, numberAttributes);
    }

    @Override
    protected List<Pair<Resource, Calendar>> getSortedValues(Map<Resource, Literal> data) {
        return data.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), toDate(e.getValue())))
                .sorted(Comparator.comparing(Pair::getRight))
                .toList();
    }

    private static Calendar toDate(Literal literal) {
        try {
            XSDDateTime xsdDateTime = (XSDDateTime) XSDDatatype.XSDdate.parse(literal.getString());
            return xsdDateTime.asCalendar();
        } catch (Exception e) {
            throw new IllegalArgumentException("Literal is not a valid xsd:date or xsd:dateTime: " + literal);
        }
    }
}
