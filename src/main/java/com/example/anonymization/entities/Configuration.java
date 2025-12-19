package com.example.anonymization.entities;


import com.example.anonymization.service.anonymizer.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.Map;

@Data
@Getter
@AllArgsConstructor
public class Configuration {
    String dataType;
    String anonymization;

    public Anonymization<? extends Configuration> createAnonymization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            int nrAttr,
            Resource anonymizationObject,
            boolean calculateKpi
    ) {
        return switch (anonymization) {
            case "generalization" -> switch (dataType) {
                case "integer", "double" -> new GeneralizationNumeric(model, property, data, nrAttr, this, anonymizationObject, calculateKpi);
                case "date" -> new GeneralizationDate(model, property, data, nrAttr, this, anonymizationObject, calculateKpi);
                case "string" -> throw new IllegalArgumentException("No Generalization possible for type string");
                default ->
                    throw new IllegalArgumentException("Invalid configuration type for object anonymization");
            };
            case "randomization" -> switch (dataType) {
                case "integer", "double" -> new RandomizationNumeric(model, property, data, nrAttr, this, anonymizationObject, calculateKpi);
                case "date" -> new RandomizationDate(model, property, data, nrAttr, this, anonymizationObject, calculateKpi);
                default ->
                        throw new IllegalArgumentException("No Randomization possible for type " + dataType);
            };
            case "masking" -> new Masking(model, property, data, this, anonymizationObject);
            default ->
                    throw new IllegalArgumentException("No Anonymization implementation for " + anonymization + ": " + dataType);
        };
    }
}
