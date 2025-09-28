package com.example.anonymization.service.anonymizer;

import org.apache.jena.base.Sys;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Random;

public class RandomizationDate extends Randomization {

    @Override
    double distance(Literal a, Literal b) {
        System.out.println(literalToDate(a).getTimeInMillis() / 1_000d - literalToDate(b).getTimeInMillis() / 1_000d);
        return literalToDate(a).getTimeInMillis() / 1_000d - literalToDate(b).getTimeInMillis() / 1_000d;
    }

    @Override
    Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max) {
        int noise = (int) (new Random().nextGaussian() * distance);
        if (literalToDate(value).getTimeInMillis() / 1_000d + noise > literalToDate(max).getTimeInMillis() / 1_000d ||
                literalToDate(value).getTimeInMillis() / 1_000d + noise < literalToDate(min).getTimeInMillis() / 1_000d) {
            noise *= -1;
        }
        Calendar noisyDate = literalToDate(value);
        noisyDate.add(Calendar.SECOND, noise);
        return ResourceFactory.createTypedLiteral(noisyDate);
    }

    @Override
    Comparator<Literal> getComparator() {
        return Comparator.comparingLong(literal -> literalToDate(literal).getTimeInMillis() / 1_000);
    }

    public static Calendar literalToDate(Literal literal) {
        try {
            XSDDateTime xsdDateTime = (XSDDateTime) XSDDatatype.XSDdate.parse(literal.getString());
            return xsdDateTime.asCalendar();
        } catch (Exception noDateTime) {
            try {
                XSDDateTime xsdDateTime = (XSDDateTime) XSDDatatype.XSDdateTime.parse(literal.getString());
                return xsdDateTime.asCalendar();
            } catch (Exception e) {
                throw new IllegalArgumentException("Literal is not a valid xsd:date or xsd:dateTime: " + literal);
            }
        }
    }

    public static double literalToNumericDate(Literal literal) {
        return literalToDate(literal).getTimeInMillis() / 1_000d;
    }
}
