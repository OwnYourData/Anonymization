package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

public class RandomizationDate extends Randomization {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private final Random random;

    public RandomizationDate(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject,
            boolean calculateKpi,
            long seed) {
        super(model, property, data, config, anonymizationObject, numberAttributes, calculateKpi);
        this.random = new Random(seed);
    }

    @Override
    double distance(Literal a, Literal b) {
        LocalDate dateA = literalToLocalDate(a);
        LocalDate dateB = literalToLocalDate(b);
        return ChronoUnit.DAYS.between(dateA, dateB);
    }

    @Override
    Literal createRandomizedLiteral(Literal value, double distance, Literal min, Literal max) {
        int noiseDays = Integer.MAX_VALUE;

        long valueDay = literalToLocalDate(value).toEpochDay();
        long minDay = literalToLocalDate(min).toEpochDay();
        long maxDay = literalToLocalDate(max).toEpochDay();

        while (valueDay + noiseDays > maxDay || valueDay + noiseDays < minDay) {
            noiseDays = (int) Math.round(random.nextGaussian() * distance);
            if (valueDay + noiseDays > maxDay || valueDay + noiseDays < minDay) {
                noiseDays *= -1;
            }
        }

        LocalDate noisyDate = literalToLocalDate(value).plusDays(noiseDays);
        String lexical = noisyDate.format(DATE_FORMATTER);

        return ResourceFactory.createTypedLiteral(lexical, XSDDatatype.XSDdate);
    }

    @Override
    Comparator<Literal> getComparator() {
        return Comparator.comparingLong(literal -> literalToLocalDate(literal).toEpochDay());
    }

    private static LocalDate literalToLocalDate(Literal literal) {
        try {
            return LocalDate.parse(literal.getLexicalForm(), DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Literal lexical form is not a valid xsd:date: " + literal, e);
        }
    }
}
