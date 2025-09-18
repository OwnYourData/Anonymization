package com.example.anonymization.service.anonymizer;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.XSD;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Random;

public class RandomizationDate extends Randomization {

    @Override
    double distance(Literal a, Literal b) {
        return toDate(a).getTimeInMillis() - toDate(b).getTimeInMillis();
    }

    @Override
    Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max) {
        int noise = (int) (new Random().nextGaussian() * distance);
        if (toDate(value).getTimeInMillis() + noise > toDate(max).getTimeInMillis() ||
                toDate(value).getTimeInMillis() + noise < toDate(min).getTimeInMillis()) {
            noise *= -1;
        }
        Calendar noisyDate = toDate(value);
        noisyDate.add(Calendar.MILLISECOND, noise);
        return ResourceFactory.createTypedLiteral(noisyDate);
    }

    @Override
    Comparator<Literal> getComparator() {
        return Comparator.comparingLong(literal -> toDate(literal).getTimeInMillis());
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
