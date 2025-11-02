package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public class Masking extends Anonymization {

    public Masking(Model model, Property property, Map<Resource, Literal> data, Configuration config) {
        super(model, property, data, config);
    }

    @Override
    public void applyAnonymization() {
        Property anonymizedValue = model.createProperty(property.getURI() + "_masked");
        data.forEach((key, value) -> {
            key.addLiteral(anonymizedValue, "*****");
        });
    }
}
