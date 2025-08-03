package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public interface Anonymization {

    void applyAnoynmization(Model model, Property property, Map<Resource, Literal> data);

    static void anonmization(Configuration config, Model model, Property property, Map<Resource, Literal> data) {
        System.out.println("Anonymization for : "+ config.getAnonymization() + " " + property);
        Anonymization anonymization = anonymizationFactoryFunction(config);
        anonymization.applyAnoynmization(model, property, data);
    }

    private static Anonymization anonymizationFactoryFunction(Configuration configuration) {
        return switch (configuration.getAnonymization()) {
            // TODO evaluate if this should be handled as strings
            case "generalization" -> switch (configuration.getDataType()) {
                case "integer" -> new GeneralizationNumeric(); // TODO include all numeric datatypes
                case "date" -> new GeneralizationDate();
                case "string" -> throw new IllegalArgumentException("No Generalization possible for type string");
                default -> new GeneralizationObject();
            };
            case "randomization" -> switch (configuration.getDataType()) {
                case "integer" -> new RandomizationNumeric(); // TODO include all numeric datatypes
                case "date" -> new RandomizationDate();
                default ->
                        throw new IllegalArgumentException("No Randomization possible for type " + configuration.getDataType());
            };
            case "masking" -> new Masking();
            default ->
                    throw new IllegalArgumentException("No Anonymization implementation for " + configuration.getAnonymization() + ": " + configuration.getDataType());
        };
    }
}
