package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.*;

import java.util.Map;

public class GeneralizationObject extends Anonymization {

    public GeneralizationObject(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
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
