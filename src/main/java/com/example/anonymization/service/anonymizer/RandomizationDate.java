package com.example.anonymization.service.anonymizer;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.time.temporal.ChronoUnit;

public class RandomizationDate extends Randomization<Date> {

    @Override
    double distance(Literal a, Literal b) {
        Date dateA = toDate(a);
        Date dateB = toDate(b);
        LocalDate localDateA = dateA.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate localDateB = dateB.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return ChronoUnit.DAYS.between(localDateA, localDateB);
    }

    private static Date toDate(Literal literal) {
        Object value = literal.getValue();
        if (value instanceof XSDDateTime xsdDateTime) {
            return xsdDateTime.asCalendar().getTime();
        }
        throw new IllegalArgumentException(
                "Literal is not a valid xsd:date or xsd:dateTime: " + literal
        );
    }

    @Override
    Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max) {
        // TODO
        return null;
    }

    @Override
    Comparator<Literal> getComparator() {
        // TODO
        return null;
    }
}
