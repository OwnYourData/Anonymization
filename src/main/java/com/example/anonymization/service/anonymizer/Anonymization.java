package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.KpiService;
import jakarta.validation.constraints.NotNull;
import org.apache.jena.rdf.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static java.lang.StrictMath.floor;
import static java.lang.StrictMath.pow;

public abstract class Anonymization<T extends Configuration> {

    private static final Logger logger = LogManager.getLogger(Anonymization.class);

    @NotNull
    Model model;
    @NotNull
    Property property;
    @NotNull
    Map<Resource, RDFNode> data;
    @NotNull
    T config;
    @NotNull
    Resource anonymizationObject;
    int numberBuckets;
    boolean calculateKpi;

    Anonymization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            T config,
            Resource anonymizationObject,
            long numberAttributes,
            boolean calculateKpi) {
        this(model, property, data, config, anonymizationObject);
        this.numberBuckets = calculateNumberOfBuckets(data.size(), numberAttributes);
        this.calculateKpi = calculateKpi;
    }

    Anonymization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            T config,
            Resource anonymizationObject) {
        this.model = model;
        this.property = property;
        this.data = data;
        this.config = config;
        this.anonymizationObject = anonymizationObject;
    }

    abstract void applyAnonymization();

    public void anonymization() {
        logger.info("Starting anonymization of attribute: {}", property.getLocalName());
        data.entrySet().removeIf(entry -> entry.getValue() == null);
        if (calculateKpi) {
            KpiService.addAttributeInformation(
                    model,
                    property,
                    numberBuckets,
                    config.getAnonymization(),
                    anonymizationObject);
        }
        applyAnonymization();
    }

    static int calculateNumberOfBuckets(long dataSize, long numberAttributes) {
        return (int) floor(
                1.0 / pow(
                        1.0 - pow(1.0 - pow(0.99, 1.0 / dataSize), 1.0 / dataSize),
                        1.0 / numberAttributes));
    }
}
