package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

import static java.lang.StrictMath.floor;
import static java.lang.StrictMath.pow;

public interface Anonymization {

    void applyAnonymization(Model model, Property property, Map<Resource, Literal> data, long numberAttributes);

    static void anonymization(Configuration config, Model model, Property property, Map<Resource, Literal> data, int nrAnonymizeAttributes) {
        System.out.println("Anonymization for : "+ config.getAnonymization() + " " + property);
        Anonymization anonymization = anonymizationFactoryFunction(config);
        anonymization.applyAnonymization(model, property, data, nrAnonymizeAttributes);
    }

    private static Anonymization anonymizationFactoryFunction(Configuration configuration) {
        return switch (configuration.getAnonymization()) {
            // TODO evaluate if this should be handled as strings
            case "generalization" -> switch (configuration.getDataType()) {
                case "integer", "double" -> new GeneralizationNumeric();
                case "date" -> new GeneralizationDate();
                case "string" -> throw new IllegalArgumentException("No Generalization possible for type string");
                default -> new GeneralizationObject();
            };
            case "randomization" -> switch (configuration.getDataType()) {
                case "integer", "double" -> new RandomizationNumeric();
                case "date" -> new RandomizationDate();
                default ->
                        throw new IllegalArgumentException("No Randomization possible for type " + configuration.getDataType());
            };
            case "masking" -> new Masking();
            default ->
                    throw new IllegalArgumentException("No Anonymization implementation for " + configuration.getAnonymization() + ": " + configuration.getDataType());
        };
    }

    static int calculateNumberOfBuckets(long dataSize, long numberAttributes) {
        return (int) floor(
                1.0 / pow(
                        1.0 - pow(1.0 - pow(0.99, 1.0 / dataSize), 1.0 / dataSize),
                        1.0 / numberAttributes
                )
        );
    }
}
