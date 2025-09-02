package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KpiService {

    public static void addKpiObject(Model model, Resource anonymizationObject, List<Property> attributes, Map<Property, Configuration> configurations) {
        Resource kpiObject = model.createResource(OntologyService.SOYA_URL + "kpiObject");
        Property property = model.createProperty(OntologyService.SOYA_URL + "kpis");
        anonymizationObject.addProperty(property, kpiObject);

        Property kAnonymity = model.createProperty(OntologyService.SOYA_URL, "kAnonymity");
        kpiObject.addLiteral(kAnonymity, calculateKAnonymity(model, attributes, configurations));
    }

    public static void addNrBuckets(Model model, Property property, int numberAttributes) {
        Resource kpiObject = model.createResource(OntologyService.SOYA_URL + "kpiObject");
        Property numberAttrProperty = model.createProperty(OntologyService.SOYA_URL + property.getLocalName() + "NumberAttributes");
        kpiObject.addLiteral(numberAttrProperty, numberAttributes);
    }

    private static int calculateKAnonymity(Model model, List<Property> attributes, Map<Property, Configuration> configurations) {

        // TODO value for generalization

        // TODO value for classification

        // TODO boolean map of equality --> first all to true, then for each attributes set value to false if not similar
        return 0;
    }

    private static void getGroups(Model model, Resource anonymizationObject, List<Property> attributes, Map<Property, Configuration> configurations) {
        List<Property> generalizingAttributes = attributes.stream()
                .filter(attr -> configurations.get(attr).getAnonymization().equals("generalization"))
                .toList();

        String queryString = createGroupQuery(anonymizationObject, generalizingAttributes);
    }

    private static String createGroupQuery(Resource anonymizationObject, List<Property> generalizingAttributes) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT (GROUP_CONCAT(?value; SEPARATOR=\", \") AS ?values)\n")
                .append("WHERE {\n")
                .append("?object a ")
                .append(anonymizationObject)
                .append(".");
        generalizingAttributes.forEach(attr ->
                queryString.append("?object ")
                        .append(attr.getLocalName())
                        .append(" ")
        );
        queryString.append("}\n")
                .append("GROUP BY");
        generalizingAttributes.forEach(attr -> queryString.append(" ").append(attr.getLocalName()));
        return queryString.toString();
    }
}
