package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public class GeneralizationObject extends Anonymization {

    public GeneralizationObject(
            Model model,
            Property property,
            Map<Resource, Literal> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject
    ) {
        super(model, property, data, config, anonymizationObject, numberAttributes);
    }

    @Override
    public void applyAnonymization() {
        // TODO
    }
}
