package com.example.anonymization.data;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueryService {

    public static final String SOYA_URL = "http://ns.ownyourdata.eu/ns/soya-context/";

    /**
     * Fetches the configuration from an input model
     * @param model the input model
     * @return configuration representation with object, attribute, datatype and anonymization type
     */
    public static List<ConfigurationResult> getConfigurations(Model model) {
        List<ConfigurationResult> configurations = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(QueryBuldingService.createConfigQuery(), model)) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution solution = rs.nextSolution();
                configurations.add(new  ConfigurationResult(
                        solution.getResource("?anonymizationObject"),
                        ResourceFactory.createProperty(solution.getResource("?attribute").getURI()),
                        solution.getResource("?datatype"),
                        solution.getLiteral("?anonymization")
                ));
            }
        }
        return  configurations;
    }

    /**
     *
     * @param model the input model
     * @param attributes attributes for which data should be fetched
     * @param objectType the type for which data should be fetched
     * @return mapping of resources of the object type with their attribute data
     */
    public static Map<Resource, Map<Property, Literal>> getData(Model model, Set<Property> attributes, Resource objectType) {
        Query query = QueryBuldingService.createDataModelQuery(attributes, objectType).asQuery();
        Map<Resource, Map<Property, Literal>> results = new HashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                Map<Property, Literal> attributValues = new HashMap<>();
                attributes.forEach(attr -> {
                    Literal value = solution.getLiteral("_" + attr.getLocalName());
                    if (value != null) {
                        attributValues.put(attr, value);
                    }
                });
                results.put(solution.getResource("object"), attributValues);
            }
        }
        return results;
    }

    /**
     * Extracts the parameters to with anonymization should be applied --> config is defined and the attribute is used
     * at least for one object
     * @param model the input model
     * @param configs list of configurations
     * @param objectType definition of the object type to which anonymization is applied
     */
    public static Set<Property> getAttributes(Model model, Set<Property> configs, Resource objectType) {
        Query query = QueryBuldingService.createAttributeQuery(configs, objectType).asQuery();
        Set<Property> properties = new HashSet<>();
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

    /**
     * Deletes the original values that are now replaced with anonymized values
     * @param model the input model
     * @param attributes the attribute that are removed
     * @param objectType the object type for which the data is removed
     */
    public static void deleteOriginalAttributes(Model model, Set<Property> attributes, Resource objectType) {
        UpdateRequest updateRequest = QueryBuldingService.deleteOriginalAttributeQuery(attributes, objectType).asUpdate();
        UpdateAction.execute(updateRequest, model);
    }

    /**
     * Extracts the randomized attribtue and the original values
     * @param model the input model
     * @param anonymizationObject the object type for which the data is returned
     * @param property the randomization property
     * @return randomization representation as a list of objects with their randomized and original attribute
     */
    public static List<RandomizationResult> getRandomizationResults(Model model, Resource anonymizationObject, Property property) {
        Query query = QueryBuldingService.createRandomizationQuery(model, anonymizationObject, property).asQuery();
        List<RandomizationResult> results = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                results.add(new RandomizationResult(
                        sol.getResource("object"),
                        sol.getLiteral("original"),
                        sol.getLiteral("randomized")
                ));
            }
        }
        return results;
    }

    /**
     * Extracts a group representation of the generalization result
     * @param model the input model
     * @param anonymizationObject the object type for which the data is returned
     * @param properties set of properties by which the result is grouped
     * @return groups of resources with the same attributes
     */
    public static List<Set<Resource>> getGeneralizationGroups(Model model, Resource anonymizationObject, Set<Property> properties) {
        ParameterizedSparqlString queryString = QueryBuldingService.createGroupsQuery(properties, anonymizationObject);
        Query query = queryString.asQuery();
        List<Set<Resource>> results = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                results.add(
                        Arrays.stream(String.valueOf(sol.get("values"))
                                .split(",")).map(uri -> model.createResource(uri.trim()))
                                .collect(Collectors.toSet())
                );
            }
        }
        return results;
    }

    public static Map<Resource, Map<Property, Literal>> getAllData(Model model, Resource objectType) {
        Set<Property> attributes = getAllAttributes(model, objectType);
        return getData(model, attributes, objectType);
    }

    public static Map<Resource, Map<Property, List<Literal>>> getAllRandomizationData(Model model, Resource objectType, Set<Property> properties) {
        Set<Property> attributes = getAllAttributes(model, objectType);
        Map<Resource, Map<Property, List<Literal>>> results = new HashMap<>();
        for (Property property : attributes) {
            List<RandomizationResult> randomizationResults = getRandomizationResults(model, objectType, property);
        }
        return results;
    }

    private static Set<Property> getAllAttributes(Model model, Resource objectType) {
        Set<Property> properties = new HashSet<>();
        String queryString = """
                SELECT ?predicate
                WHERE {
                  ?s a ?objectType ; ?predicate ?o .
                }
                """;
        ParameterizedSparqlString pss = new ParameterizedSparqlString(queryString);
        pss.setParam("objectType", objectType);
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), model)) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution solution = rs.nextSolution();
                properties.add(ResourceFactory.createProperty(solution.getResource("?predicate").getURI()));
            }
        }
        return properties;
    }

    public record ConfigurationResult(Resource object, Property attribute, Resource datatype, Literal anonymization) {}
    public record RandomizationResult(Resource object, Literal original, Literal randomized) {}
}
