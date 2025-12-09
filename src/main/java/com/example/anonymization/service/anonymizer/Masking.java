package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.*;

import java.util.Map;

public class Masking extends Anonymization<Configuration> {


    public Masking(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            Configuration config,
            Resource anonymizationObject
    ) {
        super(model, property, data, config, anonymizationObject);
    }

    @Override
    public void applyAnonymization() {
        Property anonymizedValue = model.createProperty(property.getURI() + "_masked");
        data.forEach((key, value) -> {
            key.addLiteral(anonymizedValue, "*****");
        });
    }
}
