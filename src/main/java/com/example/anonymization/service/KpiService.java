package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.exceptions.AnonymizationException;
import com.example.anonymization.service.anonymizer.RandomizationDate;
import com.example.anonymization.data.QueryService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KpiService {

    private static final String KPI_OBJECT_URI = QueryService.SOYA_URL + "kpi";

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

        Property kAnonymity = model.createProperty(QueryService.SOYA_URL, "kAnonymity");
        kpiObject.addLiteral(kAnonymity, calculateKAnonymity(model, anonymizationObject, attributes, configurations));
    }

    /**
     * Adds the number of buckets used in the anonymization for an attribute to the KPI object in the model.
     * @param model Model to which the KPI object is added
     * @param property Property for which the number of buckets is added
     * @param numberAttributes Number of buckets used in the anonymization
     */
    public static void addNrBuckets(Model model, Property property, int numberAttributes, Resource anonymizationObject) {
        Resource kpiObject = model.createResource(KPI_OBJECT_URI + anonymizationObject.getLocalName());
        Property numberAttrProperty = model.createProperty(
                QueryService.SOYA_URL + property.getLocalName() + "NumberAttributes"
        );
        kpiObject.addLiteral(numberAttrProperty, numberAttributes);
    }

    private static int calculateKAnonymity(
            Model model,
            Resource anonymizationObject,
            Set<Property> attributes, Map<Property, Configuration> configurations
    ) {
        try {
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
        } catch (Exception ex) {
            throw new AnonymizationException("Error calculating k-anonymity: " + ex.getMessage());
        }
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
        randomizationResults.forEach(randomization -> {
            if (randomization.original() != null) {
                if (date) {
                    distances.add(Math.abs(
                            RandomizationDate.literalToNumericDate(randomization.original()) -
                            RandomizationDate.literalToNumericDate(randomization.randomized())
                    ));
                    randomizedData.put(
                            randomization.object(), RandomizationDate.literalToNumericDate(randomization.randomized())
                    );
                } else {
                    distances.add(Math.abs(
                            randomization.original().getDouble() - randomization.randomized().getDouble()
                    ));
                    randomizedData.put(randomization.object(), randomization.randomized().getDouble());
                }
            } else {
                nullValues.add(randomization.object());
            }
        });
        Map<Resource, Set<Resource>> similarity = new HashMap<>();
        if (!distances.isEmpty()) {
            Collections.sort(distances);
            int index = (int) Math.ceil(0.95 * distances.size()) - 1;
            double q95 = distances.get(Math.max(0, index));
            findSimilarValues(similarity, randomizedData, q95);
        }
        nullValues.forEach(obj -> similarity.put(obj, nullValues));
        return similarity;
    }

    private static void findSimilarValues(
            Map<Resource, Set<Resource>> similarity,
            Map<Resource, Double> randomizedData,
            double benchmark
    ) {
        List<Map.Entry<Resource, Double>> objects = new ArrayList<>(randomizedData.entrySet());
        objects.sort(Map.Entry.comparingByValue());

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

            similarity.put(object, similarValues);
        }
    }
}
