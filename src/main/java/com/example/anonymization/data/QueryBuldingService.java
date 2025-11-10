package com.example.anonymization.data;

import com.example.anonymization.service.KpiService;
import com.example.anonymization.service.anonymizer.Generalization;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Set;

import static com.example.anonymization.data.QueryService.SOYA_URL;

public class QueryBuldingService {

    static String createConfigQuery() {
        return """
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                    PREFIX soya: <https://w3id.org/soya/ns#>
                    SELECT ?anonymizationObject ?property ?datatype ?anonymization WHERE {
                      ?overlay a soya:OverlayClassification .
                      ?overlay soya:onBase ?anonymizationObject .
                      ?property rdfs:domain ?anonymizationObject .
                      ?property rdfs:range ?datatype .
                      ?property <https://w3id.org/soya/ns#classification> ?anonymization .
                    }
                """;
    }

    static ParameterizedSparqlString createDataModelQuery(Set<Property> properties, Resource anonymizationObject) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("SELECT ?object");
        properties.forEach(property -> queryString.append(" ?_" + property.getLocalName()));
        queryString.append("\nWHERE {\n");
        queryString.append("  ?object a ?objectType .\n");
        properties.forEach(property -> {
            queryString.append("  OPTIONAL { ?object ?" + property.getLocalName() + " ?_" + property.getLocalName() + " ");
            queryString.append("FILTER(isLiteral(?_" + property.getLocalName() + ")) }\n");
            queryString.setParam(property.getLocalName(), property);
        });
        queryString.append("}");
        queryString.setParam("objectType",  anonymizationObject);
        return queryString;
    }

    static ParameterizedSparqlString createKpiDataQuery(Set<Property> properties, Resource kpiObject) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("SELECT");
        properties.forEach(property -> queryString.append(" ?_" + property.getLocalName()));
        queryString.append("\nWHERE {\n");
        properties.forEach(property -> {
            queryString.append("  OPTIONAL { ?object ?" + property.getLocalName() + " ?_" + property.getLocalName() + " ");
            queryString.append("FILTER(isLiteral(?_" + property.getLocalName() + ")) }\n");
            queryString.setParam(property.getLocalName(), property);
        });
        queryString.append("}");
        queryString.setParam("object", kpiObject);
        return queryString;
    }

    static ParameterizedSparqlString createPropertyQuery(Set<Property> configs, Resource anonymizationObject) {
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

    static ParameterizedSparqlString deleteOriginalPropertyQuery(Set<Property> properties, Resource objectType) {
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

    static ParameterizedSparqlString creatGeneralizationData(Set<Property> properties, Resource anonymizationObject) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("SELECT ?object");
        properties.forEach(property -> queryString.append(" ?_min_" + property.getLocalName() + " ?_max_" + property.getLocalName()));
        queryString.append("\nWHERE {\n");
        queryString.append("  ?object a ?objectType .\n");
        properties.forEach(property -> {
            queryString.append("  OPTIONAL { ?object ?" + property.getLocalName() + " ?_" + property.getLocalName() + ".\n");
            queryString.append(" ?_" + property.getLocalName() + " <" + Generalization.RDF_MAX + "> ?_max_" + property.getLocalName() + ".\n");
            queryString.append(" ?_" + property.getLocalName() + " <" + Generalization.RDF_MIN + "> ?_min_" + property.getLocalName() + ".\n");
            queryString.append("}\n");
            queryString.setParam(property.getLocalName(), property);
        });
        queryString.append("}");
        queryString.setParam("objectType",  anonymizationObject);
        return queryString;
    }

    static ParameterizedSparqlString createKAnonymityQuery(Resource kpiObject, Property kAnonymity) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("SELECT ?value WHERE {\n");
        queryString.append("?kpiObject ?kAnonymity ?value .\n}");
        queryString.setParam("kpiObject", kpiObject);
        queryString.setParam("kAnonymity", kAnonymity);
        return queryString;
    }

    static ParameterizedSparqlString createAttributeInformationQuery(
            Resource kpiObject,
            Property hasAttribute,
            Property nrBuckets,
            Property anonymization
    ) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.append("SELECT ?attribute ?anonymization ?nrBuckets WHERE {\n");
        queryString.append("?kpiObject ?hasAttribute ?attribute .\n");
        queryString.append("OPTIONAL { ?attribute ?nrBucketsAttribute ?nrBuckets . }\n");
        queryString.append("?attribute ?anonymizationAttribute ?anonymization .\n");
        queryString.append("}\n");
        queryString.setParam("kpiObject", kpiObject);
        queryString.setParam("hasAttribute", hasAttribute);
        queryString.setParam("nrBucketsAttribute", nrBuckets);
        queryString.setParam("anonymizationAttribute", anonymization);
        return queryString;
    }

}
