package com.example.anonymization.service;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OntologyService {

    public static final String SOYA_URL = "http://ns.ownyourdata.eu/ns/soya-context/";

    public static Map<Resource, Map<Property, Literal>> extractDataFromModel(Model model, List<Property> attributes, String objectType) {
        String queryString = createQueryForAttributes(attributes, objectType);
        Query query = QueryFactory.create(queryString);
        Map<Resource, Map<Property, Literal>> results = new HashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                Map<Property, Literal> attributValues = new HashMap<>();
                attributes.forEach(attr -> attributValues.put(attr, solution.getLiteral(attr.getLocalName())));
                results.put(solution.getResource("object"), attributValues);
            }
        }
        return results;
    }

    public static void deleteOldValues(Model model, List<Property> attributes, String objectType) {
        String deleteQuery = createDelteQuery(attributes, objectType);
        UpdateRequest updateRequest = UpdateFactory.create(deleteQuery);
        UpdateAction.execute(updateRequest, model);
    }

    /**
     * Extracts the parameters to with anonymization should be applied --> config is defined and the attribute is used
     * at least for one object
     * @param model the input model
     * @param configs list of configurations
     * @param objectType definition of the object type to which anonymization is applied
     */
    public static List<Property> extractAttributesForAnonymization(Model model, Set<String> configs, String objectType) {
        String attributeQuery = createAttributeQuery(configs, objectType);
        Query query = QueryFactory.create(attributeQuery);
        List<Property> properties = new LinkedList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                if (solution.getLiteral("used").getBoolean()) {
                    properties.add(model.getProperty(solution.getResource("predicate").getURI()));
                }
            }
        }
        return properties;
    }

    private static String createQueryForAttributes(List<Property> attributes, String objectType) {
        // TODO check why ? is cutting the first char afterwards (not the case for delte query)
        StringBuilder queryString = new StringBuilder();
        queryString.append("PREFIX oyd: <" + SOYA_URL + "> \n")
                .append("SELECT ?object ");
        attributes.forEach(attr -> queryString.append("?").append(attr.getLocalName()).append(" "));
        queryString.append("\n")
                .append("WHERE { ?object ");
        attributes.forEach(attr -> queryString
                .append("<").append(attr).append("> ?").append(attr.getLocalName()).append(" ;\n"));
        queryString.append("a oyd:").append(objectType).append(".\n}");
        return queryString.toString();
    }

    private static String createDelteQuery(List<Property> attributes, String objectType) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("PREFIX oyd: <\" + SOYA_URL + \"> \n")
                .append("DELETE {\n");
        attributes.forEach(attr -> queryString
                .append("?object ")
                .append("<").append(attr).append("> ")
                .append("?").append(attr.getLocalName()).append(".\n"));
        queryString.append("}\nWHERE {\n")
                .append("?object a oyd:").append(objectType).append(".\n");
        attributes.forEach(attr -> queryString
                .append("OPTIONAL { ?object ")
                .append("<").append(attr).append("> ")
                .append("?").append(attr.getLocalName()).append(" .}\n"));
        queryString.append("}");
        return queryString.toString();
    }

    private static String createAttributeQuery(Set<String> configs, String objectType) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("PREFIX oyd: <\" + SOYA_URL + \"> \n")
                .append("SELECT ?predicate (EXISTS {\n?s a oyd:").append(objectType)
                .append(" ; ?predicate ?o .\n} AS ?used)\n")
                .append("WHERE { VALUES ?predicate { \n");
        configs.forEach(config -> queryString.append("oyd:").append(config).append("\n"));
        queryString.append("}}");
        return queryString.toString();
    }
}
