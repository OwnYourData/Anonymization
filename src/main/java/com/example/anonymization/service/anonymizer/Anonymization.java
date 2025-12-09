package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.KpiService;
import jakarta.validation.constraints.NotNull;
import org.apache.jena.rdf.model.*;

import java.util.Map;

import static java.lang.StrictMath.floor;
import static java.lang.StrictMath.pow;

public abstract class Anonymization<T extends Configuration> {

    @NotNull Model model;
    @NotNull Property property;
    @NotNull Map<Resource, RDFNode> data;
    @NotNull T config;
    @NotNull Resource anonymizationObject;
    int numberBuckets;

    Anonymization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            T config,
            Resource anonymizationObject,
            long numberAttributes
    ) {
        this(model, property, data, config, anonymizationObject);
        this.numberBuckets = calculateNumberOfBuckets(data.size(), numberAttributes);
    }

    Anonymization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            T config,
            Resource anonymizationObject
    ) {
        this.model = model;
        this.property = property;
        this.data = data;
        this.config = config;
        this.anonymizationObject = anonymizationObject;
    }

    abstract void applyAnonymization();

    public void anonymization() {
        data.entrySet().removeIf(entry -> entry.getValue() == null);
        System.out.println("Anonymization for : "+ config.getAnonymization() + " " + property);
        KpiService.addAttributeInformation(
                model,
                property,
                numberBuckets,
                config.getAnonymization(),
                anonymizationObject
        );
        applyAnonymization();
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
