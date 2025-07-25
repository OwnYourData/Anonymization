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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OntologyService {

    public static Map<Resource, Map<Property, Literal>> extractDataFromModel(Model model, List<Property> attributes, String objectType) {
        String queryString = createQueryForAttributes(attributes, objectType);
        Query query = QueryFactory.create(queryString);
        Map<Resource, Map<Property, Literal>> results = new HashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                Map<Property, Literal> attributValues = new HashMap<>();
                attributes.forEach(attr -> attributValues.put(attr, solution.getLiteral(attr.toString())));
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

    private static String createQueryForAttributes(List<Property> attributes, String objectType) {
        // TODO check why ? is cutting the first char afterwards (not the case for delte query)
        StringBuilder queryString = new StringBuilder();
        queryString.append("PREFIX oyd: <http://ns.ownyourdata.eu/ns/soya-context> \n");
        queryString.append("SELECT ?object ");
        attributes.forEach(attr -> queryString.append("?a").append(attr.getLocalName()).append(" "));
        queryString.append("\n");
        queryString.append("WHERE { ?object ");
        attributes.forEach(attr -> queryString.append("oyd:").append(attr).append(" ?a").append(attr.getLocalName()).append(" ;\n"));
        queryString.append("a oyd:").append(objectType).append(".\n}");

        return queryString.toString();
    }

    private static String createDelteQuery(List<Property> attributes, String objectType) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("PREFIX oyd: <http://ns.ownyourdata.eu/ns/soya-context> \n");
        queryString.append("DELETE {\n");
        attributes.forEach(attr -> queryString.append("?object oyd:").append(attr).append(" ?").append(attr).append(".\n"));
        queryString.append("}\nWHERE {\n");
        queryString.append("?object a oyd:").append(objectType).append(".\n");
        attributes.forEach(attr -> queryString.append("OPTIONAL { ?object oyd:").append(attr).append(" ?").append(attr).append(" .}\n"));
        queryString.append("}");
        return queryString.toString();
    }
}
