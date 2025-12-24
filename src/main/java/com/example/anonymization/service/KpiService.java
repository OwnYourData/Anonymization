package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.service.anonymizer.RandomizationDate;
import com.example.anonymization.data.QueryService;
import com.example.anonymization.service.anonymizer.RandomizationDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KpiService {

    private static final Logger logger = LoggerFactory.getLogger(KpiService.class);

    public static final String KPI_OBJECT_URI = QueryService.SOYA_URL + "kpi";
    public static final String K_ANONYMITY = QueryService.SOYA_URL + "kanonymity";
    public static final String HAS_ATTRIBUTE_URI = QueryService.SOYA_URL + "hasAttribute";
    public static final String ANONYMIZATION_TYP_URI = QueryService.SOYA_URL + "anonymizationTyp";
    public static final String NR_BUCKETS_URI = QueryService.SOYA_URL + "nrBucketsUsed";

    /**
     * Adds a KPI object to the model containing the k-anonymity value for the given anonymization object.
     * @param model Model to which the KPI object is added
     * @param anonymizationObject Anonymization object for which the KPI object is created
     * @param attributes Attributes involved in the anonymization
     * @param configurations Configurations for the attributes
     */
    public static void addKpiObject(
            Model model,
            Resource anonymizationObject,
            Set<Property> attributes,
            Map<Property, Configuration> configurations
    ) {
        Resource kpiObject = model.createResource(KPI_OBJECT_URI + anonymizationObject.getLocalName());
        Property property = model.createProperty(QueryService.SOYA_URL + "kpis");
        anonymizationObject.addProperty(property, kpiObject);

        Property kAnonymity = model.createProperty(K_ANONYMITY);
        kpiObject.addLiteral(kAnonymity, calculateKAnonymity(model, anonymizationObject, attributes, configurations));
    }

    /**
     * Adds the number of buckets used in the anonymization for an attribute to the KPI object in the model.
     * @param model Model to which the KPI object is added
     * @param property Property for which the number of buckets is added
     * @param nrBucketsUsed Number of buckets used in the anonymization
     * @param anonymizationType Anonymization Implementation applied
     * @param anonymizationObject Object for which the value should be added
     */
    public static void addAttributeInformation(
            Model model,
            Property property,
            long nrBucketsUsed,
            String anonymizationType,
            Resource anonymizationObject
    ) {
        Resource kpiObject = model.createResource(KPI_OBJECT_URI + anonymizationObject.getLocalName());
        model.add(kpiObject, model.createProperty(HAS_ATTRIBUTE_URI), property);
        model.add(property, model.createProperty(ANONYMIZATION_TYP_URI), anonymizationType);
        if (!anonymizationType.equals("masking")) {
            model.addLiteral(property, model.createProperty(NR_BUCKETS_URI), nrBucketsUsed);
        }
    }

    private static int calculateKAnonymity(
            Model model,
            Resource anonymizationObject,
            Set<Property> attributes, Map<Property, Configuration> configurations
    ) {
        logger.info("Calculating k-anonymity for object: {}", anonymizationObject.getURI());
        Map<Resource, Set<Resource>> similarValues = new HashMap<>();
        List<Set<Resource>> groups = QueryService.getGeneralizationGroups(model, anonymizationObject, attributes);
        groups.forEach(group -> group.forEach(
                resource -> similarValues.put(resource, new HashSet<>(group))
        ));

        attributes.stream().filter(attr -> configurations.get(attr).getAnonymization().equals("randomization"))
                .forEach(randomization -> {
                    Map<Resource, Set<Resource>> similarity = getSimilarValues(
                            model,
                            anonymizationObject,
                            randomization,
                            configurations.get(randomization).getDataType().equals("date")
                    );
                    similarValues.keySet().forEach(
                            resource -> similarValues.get(resource).retainAll(similarity.get(resource))
                    );
                });
        return similarValues.values().stream().mapToInt(Set::size).min().orElse(0);
    }

    private static Map<Resource, Set<Resource>> getSimilarValues(
            Model model,
            Resource resource,
            Property property,
            boolean date
    ) {
        List<QueryService.RandomizationResult> randomizationResults =
                    QueryService.getRandomizationResults(model, resource, property);

        List<Double> distances = new ArrayList<>();
        Map<Resource, Double> randomizedData = new HashMap<>();
        Set<Resource> nullValues = new HashSet<>();
        NavigableMap<Double, Set<Resource>> originalValuesMap = new TreeMap<>();

        randomizationResults.forEach(randomization -> {
            if (randomization.original() != null) {
                if (date) {
                    distances.add(Math.abs(
                            RandomizationDateTime.literalToNumericDate(randomization.original()) -
                            RandomizationDateTime.literalToNumericDate(randomization.randomized())
                    ));
                    randomizedData.put(
                            randomization.object(), RandomizationDateTime.literalToNumericDate(randomization.randomized())
                    );
                    originalValuesMap.computeIfAbsent(
                            RandomizationDateTime.literalToNumericDate(randomization.original()), _ -> new HashSet<>()
                    ).add(randomization.object());
                } else {
                    distances.add(Math.abs(
                            randomization.original().getDouble() - randomization.randomized().getDouble()
                    ));
                    randomizedData.put(randomization.object(), randomization.randomized().getDouble());
                    originalValuesMap.computeIfAbsent(
                            randomization.original().getDouble(), _ -> new HashSet<>()
                    ).add(randomization.object());
                }
            } else {
                nullValues.add(randomization.object());
            }
        });
        Map<Resource, Set<Resource>> similarity = new HashMap<>();
        if (!distances.isEmpty()) {
            double benchmark = distances.stream().mapToDouble(Double::doubleValue).sum() * 2 / distances.size();
            randomizedData.forEach((key, value) ->
                    similarity.put(key, findInRange(value, benchmark, originalValuesMap)));
        }
        nullValues.forEach(obj -> similarity.put(obj, nullValues));
        return similarity;
    }

    public static Set<Resource> findInRange(
            double randomizedValue,
            double benchmark,
            NavigableMap<Double, Set<Resource>> originalValuesMap
    ) {
        double minInclusive = randomizedValue - benchmark;
        double maxInclusive = randomizedValue + benchmark;
        NavigableMap<Double, Set<Resource>> sub = originalValuesMap.subMap(minInclusive, true, maxInclusive, true);

        Set<Resource> result = new HashSet<>();
        for (Set<Resource> set : sub.values()) {
            result.addAll(set);
        }
        return result;
    }
}
