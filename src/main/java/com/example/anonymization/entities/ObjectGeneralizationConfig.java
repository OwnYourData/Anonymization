package com.example.anonymization.entities;

import com.example.anonymization.service.anonymizer.GeneralizationObject;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.List;
import java.util.Map;

@Getter
public class ObjectGeneralizationConfig extends Configuration {

    List<String> attributeOrder;

    public ObjectGeneralizationConfig(String dataType, List<String> attributeOrder) {
        super(dataType, "generalization");
        this.attributeOrder = attributeOrder;
    }

    @Override
    public GeneralizationObject createAnonymization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            int nrAttr,
            Resource anonymizationObject
    ) {
        return new GeneralizationObject(model, property, data, nrAttr, this, anonymizationObject);
    }
}
