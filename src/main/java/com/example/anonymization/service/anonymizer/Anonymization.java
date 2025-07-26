package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public interface Anonymization {

    void applyAnoynmization(Model model, Property property, Map<Resource, Literal> data);

    public static void test(Configuration config, Model model, Property property, Map<Resource, Literal> data) {
        System.out.println("Anonymization for : "+ config.getAnonymization() + " " + property);
        // TODO creates the right Anonymization class and calls applyAnonymizaation
    }

    /*
    Property min = anonymizedModel.createProperty(NS, "min");
    Property max = anonymizedModel.createProperty(NS, "max");

    anonymized.forEach((key, value) -> {
        Resource res = anonymizedModel.createResource(key);
        res.addLiteral(min, value[0]);
        res.addLiteral(max, value[1]);
    });
    */
}
