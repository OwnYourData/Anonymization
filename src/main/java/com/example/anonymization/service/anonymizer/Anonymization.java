package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.KpiService;
import lombok.AllArgsConstructor;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

import static java.lang.StrictMath.floor;
import static java.lang.StrictMath.pow;

@AllArgsConstructor
public abstract class Anonymization {

    Model model;
    Property property;
    Map<Resource, Literal> data;
    long numberAttributes;
    Configuration config;
    Resource anonymizationObject;

    Anonymization(Model model, Property property, Map<Resource, Literal> data, Configuration config) {
        this.model = model;
        this.property = property;
        this.data = data;
        this.config = config;
    }

    abstract void applyAnonymization();

    public void anonymization() {
        data.entrySet().removeIf(entry -> entry.getValue() == null);
        System.out.println("Anonymization for : "+ config.getAnonymization() + " " + property);
        KpiService.addAttributeInformation(model, property, numberAttributes, config.getAnonymization(), anonymizationObject);
        applyAnonymization();
    }

    public static Anonymization anonymizationFactoryFunction(
            Configuration config,
            Model model,
            Property property,
            Map<Resource, Literal> data,
            int nrAttr,
            Resource anonymizationObject
    ) {
        return switch (config.getAnonymization()) {
            case "generalization" -> switch (config.getDataType()) {
                case "integer", "double" -> new GeneralizationNumeric(model, property, data, nrAttr, config, anonymizationObject);
                case "date" -> new GeneralizationDate(model, property, data, nrAttr, config, anonymizationObject);
                case "string" -> throw new IllegalArgumentException("No Generalization possible for type string");
                default -> new GeneralizationObject(model, property, data, nrAttr, config, anonymizationObject);
            };
            case "randomization" -> switch (config.getDataType()) {
                case "integer", "double" -> new RandomizationNumeric(model, property, data, nrAttr, config, anonymizationObject);
                case "date" -> new RandomizationDate(model, property, data, nrAttr, config, anonymizationObject);
                default ->
                        throw new IllegalArgumentException("No Randomization possible for type " + config.getDataType());
            };
            case "masking" -> new Masking(model, property, data, config);
            default ->
                    throw new IllegalArgumentException("No Anonymization implementation for " + config.getAnonymization() + ": " + config.getDataType());
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
