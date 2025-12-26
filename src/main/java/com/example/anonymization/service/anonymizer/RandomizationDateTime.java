package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

public class RandomizationDateTime extends Randomization {

    private final Random random;

    public RandomizationDateTime(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject,
            boolean calculateKpi,
            long seed
    ) {
        super(model, property, data, config, anonymizationObject, numberAttributes, calculateKpi);
        this.random = new Random(seed);
    }

    @Override
    double distance(Literal a, Literal b) {
        return literalToDate(a).getTimeInMillis() / 1_000d - literalToDate(b).getTimeInMillis() / 1_000d;
    }

    @Override
    Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max) {
        int noise = Integer.MAX_VALUE;
        long dateSeconds = literalToDate(value).getTimeInMillis() / 1_000;
        long maxSeconds = literalToDate(max).getTimeInMillis() / 1_000;
        long minSeconds = literalToDate(min).getTimeInMillis() / 1_000;
        while(dateSeconds + noise > maxSeconds || dateSeconds + noise < minSeconds) {
            noise = (int) (random.nextGaussian() * distance);
            if (dateSeconds + noise > maxSeconds || dateSeconds + noise < minSeconds) {
                noise *= -1;
            }
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
