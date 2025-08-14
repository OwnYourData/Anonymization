package com.example.anonymization.service.anonymizer;

import com.example.anonymization.service.OntologyService;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public class Masking implements Anonymization {
    @Override
    public void applyAnoynmization(Model model, Property property, Map<Resource, Literal> data, long numberAttributes) {
        Property anonymizedValue = model.createProperty(OntologyService.SOYA_URL, property.getLocalName() + "Anoynmized");
        data.forEach((key, value) -> {
            key.addLiteral(anonymizedValue, "*****");
        });
    }
}
