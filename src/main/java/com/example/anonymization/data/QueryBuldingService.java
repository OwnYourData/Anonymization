package com.example.anonymization.data;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.List;
import java.util.Set;

public class QueryBuldingService {

    static String createConfigQuery() {
        return """
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                    PREFIX soya: <https://w3id.org/soya/ns#>
                    SELECT ?anonymizationObject ?attribute ?datatype ?anonymization WHERE {
                      ?overlay a soya:OverlayClassification .
                      ?overlay soya:onBase ?anonymizationObject .
                      ?attribute rdfs:domain ?anonymizationObject .
                      ?attribute rdfs:range ?datatype .
                      ?attribute <https://w3id.org/soya/ns#classification> ?anonymization .
                    }
                """;
    }

    static ParameterizedSparqlString createDataModelQuery(Set<Property> attributes, Resource anonymizationObject) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("SELECT ?object");
        attributes.forEach(attribute -> queryString.append(" ?_" + attribute.getLocalName()));
        queryString.append("\nWHERE {\n");
        queryString.append("  ?object a ?objectType .\n");
        attributes.forEach(attribute -> {
            queryString.append("  OPTIONAL { ?object ?" + attribute.getLocalName() + " ?_" + attribute.getLocalName() + " ");
            queryString.append("FILTER(isLiteral(?_" + attribute.getLocalName() + ")) }\n");
            queryString.setParam(attribute.getLocalName(), attribute);
        });
        queryString.append("}");
        queryString.setParam("objectType",  anonymizationObject);
        return queryString;
    }

    static ParameterizedSparqlString createAttributeQuery(Set<Property> configs, Resource anonymizationObject) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("SELECT ?predicate (EXISTS {\n");
        queryString.append("  ?s a ?objectType ; ?predicate ?o .\n");
        queryString.append("} AS ?used)\n");
        queryString.append("WHERE { VALUES ?predicate {");
        int i = 0;
        for (Property p : configs) {
            queryString.append(" ?p" + i);
            queryString.setParam("p" + i, p);
            i++;
        }
        queryString.append(" } }");
        queryString.setParam("objectType",  anonymizationObject);
        return queryString;
    }

    static ParameterizedSparqlString deleteOriginalAttributeQuery(Set<Property> properties, Resource objectType) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("DELETE {\n");
        for (int i = 0; i < properties.size(); i++) {
            queryString.append("  ?object ?p" + i + " ?v" + i + " .\n");
        }
        queryString.append("}\nWHERE {\n");
        queryString.append("  ?object a ?type .\n");
        int i = 0;
        for (Property property : properties) {
            queryString.append("  OPTIONAL { ?object ?p" + i + " ?v" + i + " . }\n");
            queryString.setParam("p" + i, property);
            i++;
        }
        queryString.append("}");
        queryString.setParam("type", objectType);
        return queryString;
    }

    static ParameterizedSparqlString createRandomizationQuery(Model model, Resource anonymizationObject, Property property) {
        ParameterizedSparqlString queryString =  new ParameterizedSparqlString();
        queryString.setCommandText("""
                SELECT ?object ?randomized ?original
                WHERE {
                    ?object a ?objectType .
                    OPTIONAL { ?object ?originalProperty ?original . }
                    OPTIONAL { ?object ?randomizedProperty ?randomized . }
                }
                """);
        queryString.setParam("objectType",  anonymizationObject);
        queryString.setParam("originalProperty", property);
        queryString.setIri("randomizedProperty", property.getURI() + "_randomized");
        return  queryString;
    }

    static ParameterizedSparqlString createGroupsQuery(Set<Property> properties, Resource anonymizationObject) {
        ParameterizedSparqlString queryString =  new ParameterizedSparqlString();
        queryString.append("SELECT (GROUP_CONCAT(?object; SEPARATOR=\", \") AS ?values)\n");
        queryString.append("WHERE {\n");
        queryString.append("  ?object a ?objectType .\n");
        int i = 0;
        for (Property property : properties) {
            queryString.append("  OPTIONAL { ?object ?p" + i + " ?v" + i + " . }\n");
            queryString.setIri("p" + i, property.getURI() + "_generalized");
            i++;
        }
        queryString.append("}\n");
        if (!properties.isEmpty()) {
            queryString.append("GROUP BY");
            for (int j = 0; j < i; j++) {
                queryString.append(" ?v" + j);
            }
            queryString.append("\n");
        }
        queryString.setParam("objectType", anonymizationObject);
        return queryString;
    }

}
