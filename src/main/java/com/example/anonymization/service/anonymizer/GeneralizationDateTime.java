package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;

import java.util.*;

public class GeneralizationDateTime extends Generalization<Calendar> {

    public GeneralizationDateTime(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject,
            boolean calculateKpi) {
        super(model, property, data, config, anonymizationObject, numberAttributes, calculateKpi);
    }

    @Override
    protected List<Pair<Resource, Calendar>> getSortedValues(Map<Resource, RDFNode> data) {
        return data.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), toDate(e.getValue())))
                .sorted(Comparator.comparing(Pair::getRight))
                .toList();
    }

    @Override
    protected Calendar getMedianValue(Calendar value1, Calendar value2) {
        if (value1 == null) {
            return value2;
        }
        if (value2 == null) {
            return value1;
        }
        long time1 = value1.getTimeInMillis();
        long time2 = value2.getTimeInMillis();
        long medianTime = (time1 + time2) / 2;
        Calendar medianCalendar = Calendar.getInstance();
        medianCalendar.setTimeInMillis(medianTime);
        return medianCalendar;
    }

    private static Calendar toDate(RDFNode node) {
        try {
            XSDDateTime xsdDateTime = (XSDDateTime) XSDDatatype.XSDdate.parse(node.asLiteral().getString());
            return xsdDateTime.asCalendar();
        } catch (Exception e) {
            throw new IllegalArgumentException("Node is not Literal or not a valid xsd:date or xsd:dateTime: " + node,
                    e);
        }
    }
}
