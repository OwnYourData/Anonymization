package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiService {

    // TODO implement Randomization KPI calculation for Dates

    public static void addKpiObject(Model model, Resource anonymizationObject, List<Property> attributes, Map<Property, Configuration> configurations) {
        Resource kpiObject = model.createResource(OntologyService.SOYA_URL + "kpiObject");
        Property property = model.createProperty(OntologyService.SOYA_URL + "kpis");
        anonymizationObject.addProperty(property, kpiObject);

        Property kAnonymity = model.createProperty(OntologyService.SOYA_URL, "kAnonymity");
        kpiObject.addLiteral(kAnonymity, calculateKAnonymity(model, anonymizationObject, attributes, configurations));
    }

    public static void addNrBuckets(Model model, Property property, int numberAttributes) {
        Resource kpiObject = model.createResource(OntologyService.SOYA_URL + "kpiObject");
        Property numberAttrProperty = model.createProperty(OntologyService.SOYA_URL + property.getLocalName() + "NumberAttributes");
        kpiObject.addLiteral(numberAttrProperty, numberAttributes);
    }

    private static int calculateKAnonymity(Model model, Resource anonymizationObject, List<Property> attributes, Map<Property, Configuration> configurations) {
        Map<Resource, Set<Resource>> similarValues = new HashMap<>();
        List<Set<Resource>> groups = getGroups(model, anonymizationObject, attributes, configurations);
        groups.forEach(group -> group.forEach(resource -> similarValues.put(resource, new HashSet<>(group))));

        // TODO handling for randomization if value is null
        attributes.stream().filter(attr -> configurations.get(attr).getAnonymization().equals("randomization"))
                .forEach(randomization -> {
                    Map<Resource, Set<Resource>> similarity = getSimilarValues(model, anonymizationObject, randomization);
                    similarValues.keySet().forEach(resource -> similarValues.get(resource).retainAll(similarity.get(resource)));
                });
        return similarValues.values().stream().mapToInt(Set::size).min().orElse(0);
    }

    private static List<Set<Resource>> getGroups(Model model, Resource anonymizationObject, List<Property> attributes, Map<Property, Configuration> configurations) {
        List<Property> generalizingAttributes = attributes.stream()
                .filter(attr -> configurations.get(attr).getAnonymization().equals("generalization"))
                .toList();
        Query query = QueryFactory.create(createGroupQuery(anonymizationObject, generalizingAttributes));
        List<Set<Resource>> groups = new LinkedList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                groups.add(
                        Arrays.stream(String.valueOf(solution.get("values"))
                                .split(",")).map(uri -> model.createResource(uri.trim()))
                                .collect(Collectors.toSet())
                );
            }
        }
        return groups;
    }

    private static Map<Resource, Set<Resource>> getSimilarValues(Model model, Resource resource, Property property) {
        Query query = QueryFactory.create(createAnoymizationDataQuery(resource, property));
        List<Double> distances = new ArrayList<>();
        Map<Resource, Double> randomizedData = new HashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                distances.add(solution.getLiteral("distance").getDouble());
                randomizedData.put(solution.getResource("object"), solution.getLiteral("randomized").getDouble());
            }
        }
        Collections.sort(distances);
        int index = (int) Math.ceil(0.95 * distances.size()) - 1;
        double q95 = distances.get(Math.max(0, index));
        return findSimilarValues(randomizedData, q95);
    }

    private static Map<Resource, Set<Resource>> findSimilarValues(Map<Resource, Double> randomizedData, double benchmark) {
        List<Map.Entry<Resource, Double>> objects = new ArrayList<>(randomizedData.entrySet());
        objects.sort(Map.Entry.comparingByValue());

        Map<Resource, Set<Resource>> similarityRepresentation = new HashMap<>();

        for (int i = 0; i < objects.size(); i++) {
            Resource object = objects.get(i).getKey();
            double randomizedValue = objects.get(i).getValue();
            Set<Resource> similarValues = new HashSet<>();
            similarValues.add(object);

            for (int j = i + 1; j < objects.size(); j++) {
                if (objects.get(j).getValue() - randomizedValue > benchmark) break;
                similarValues.add(objects.get(j).getKey());
            }

            for (int j = i - 1; j >= 0; j--) {
                if (randomizedValue - objects.get(j).getValue() > benchmark) break;
                similarValues.add(objects.get(j).getKey());
            }

            similarityRepresentation.put(object, similarValues);
        }
        return similarityRepresentation;
    }

    private static String createGroupQuery(Resource anonymizationObject, List<Property> generalizingAttributes) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT (GROUP_CONCAT(?object; SEPARATOR=\", \") AS ?values)\n")
                .append("WHERE {\n")
                .append("?object a <")
                .append(anonymizationObject)
                .append("> .\n");
        generalizingAttributes.forEach(attr ->
        queryString.append("OPTIONAL { ?object <")
                        .append(attr.getURI())
                        .append("_generalized> ?")
                        .append(attr.getLocalName())
                        .append(" . } \n")
        );
        queryString.append("}\n")
                .append("GROUP BY");
        generalizingAttributes.forEach(attr -> queryString.append(" ?").append(attr.getLocalName()));
        return queryString.toString();
    }

    private static String createAnoymizationDataQuery(Resource anonymizationObject, Property property) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT ?object ?randomized (ABS(?original - ?randomized) AS ?distance)\n")
                .append("WHERE {\n")
                .append("?object a <")
                .append(anonymizationObject)
                .append("> .\n")
                .append("?object <").append(property).append("> ?original .\n")
                .append("?object <").append(property.getURI()).append("_randomized> ?randomized .\n")
                .append("}");
        return queryString.toString();
    }
}
