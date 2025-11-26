package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.entities.ObjectGeneralizationConfig;
import org.apache.jena.rdf.model.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeneralizationObject extends Anonymization<ObjectGeneralizationConfig> {

    public GeneralizationObject(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            long numberAttributes,
            Configuration config,
            Resource anonymizationObject
    ) {
        super(model, property, data, (ObjectGeneralizationConfig) config, anonymizationObject, numberAttributes);
    }

    @Override
    public void applyAnonymization() {
        Property generalized = model.createProperty(property.getURI(), "_generalized");
        List<Property> attributes = config.getAttributeOrder().stream()
                .map(p -> model.getProperty("https://soya.ownyourdata.eu/AnonymisationDemo/" + p))
                .toList();
        for (Property objectProperty : attributes) {
            Map<Resource, Literal> propertyData = data.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                                Statement stmt = model.getProperty(e.getValue().asResource(), objectProperty);
                                return stmt != null && stmt.getObject().isLiteral() ?
                                        stmt.getLiteral() : model.createLiteral("");
                            }
                    ));
            if (checkIfAnonymizationIsEnough(propertyData)) {
                propertyData.forEach((key, value) -> key.addProperty(generalized, value));
                return;
            }
        }
        data.keySet().forEach(key -> key.addProperty(generalized, model.createLiteral("*****")));
    }

    private boolean checkIfAnonymizationIsEnough(Map<Resource, Literal> propertyData) {
        Collection<Long> groupCounts = propertyData.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getValue().getValue(),
                        Collectors.counting()
                ))
                .values();
        return groupCounts.size() <= numberBuckets && Collections.min(groupCounts) > data.size() * 0.5 / numberBuckets;
    }
}
