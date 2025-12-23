package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

public class RandomizationDate extends Randomization {

    public RandomizationDate(
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
    double distance(Literal a, Literal b) {
        return literalToDate(a).getTimeInMillis() / 1_000d - literalToDate(b).getTimeInMillis() / 1_000d;
    }

    @Override
    Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max) {
        int noise = Integer.MAX_VALUE;
        while(literalToDate(value).getTimeInMillis() / 1_000d + noise > literalToDate(max).getTimeInMillis() / 1_000d ||
                literalToDate(value).getTimeInMillis() / 1_000d + noise < literalToDate(min).getTimeInMillis() / 1_000d) {
            noise = (int) (new Random().nextGaussian() * distance);
            if (literalToDate(value).getTimeInMillis() / 1_000d + noise > literalToDate(max).getTimeInMillis() / 1_000d ||
                    literalToDate(value).getTimeInMillis() / 1_000d + noise < literalToDate(min).getTimeInMillis() / 1_000d) {
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
