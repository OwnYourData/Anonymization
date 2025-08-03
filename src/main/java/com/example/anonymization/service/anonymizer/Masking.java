package com.example.anonymization.service.anonymizer;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

public class Masking implements Anonymization {
    @Override
    public void applyAnoynmization(Model model, Property property, Map<Resource, Literal> data) {

    }
}
