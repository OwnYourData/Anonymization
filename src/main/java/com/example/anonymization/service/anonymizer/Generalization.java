package com.example.anonymization.service.anonymizer;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.data.QueryService;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class Generalization<T> extends Anonymization<Configuration> {

    public static final String RDF_MAX = "http://www.w3.org/2000/01/rdf-schema#max";
    public static final String RDF_MIN = "http://www.w3.org/2000/01/rdf-schema#min";

    public Generalization(
            Model model,
            Property property,
            Map<Resource, RDFNode> data,
            Configuration config,
            Resource anonymizationObject,
            long numberAttributes,
            boolean calculateKpi) {
        super(model, property, data, config, anonymizationObject, numberAttributes, calculateKpi);
    }

    @Override
    public void applyAnonymization() {
        List<Pair<Resource, T>> sortedValues = getSortedValues(data);
        List<Resource> buckets = createBuckets(model, numberBuckets, sortedValues, property);
        Map<Resource, Resource> ranges = getRanges(sortedValues, numberBuckets, buckets);
        writeToModel(model, ranges, property);
    }

    protected abstract List<Pair<Resource, T>> getSortedValues(Map<Resource, RDFNode> data);

    protected abstract T getMedianValue(T value1, T value2);

    protected Map<Resource, Resource> getRanges(
            List<Pair<Resource, T>> sortedValues,
            int numberBuckets,
            List<Resource> buckets) {
        List<Pair<Resource, Integer>> positionValues = new LinkedList<>();
        for (int i = 0; i < sortedValues.size(); i++) {
            positionValues.add(new Pair<>(
                    sortedValues.get(i).getLeft(),
                    numberBuckets * i / sortedValues.size()));
        }
        return positionValues.stream()
                .map(e -> new Pair<>(e.getLeft(), buckets.get(e.getRight())))
                .collect(Collectors.toMap(
                        Pair::getLeft,
                        Pair::getRight));
    }

    protected void writeToModel(Model model, Map<Resource, Resource> data, Property property) {
        Property generalized = model.createProperty(property.getURI(), "_generalized");
        data.forEach((key, value) -> key.addProperty(generalized, value));
    }

    protected List<Resource> createBuckets(
            Model model,
            int nrOfBuckets,
            List<Pair<Resource, T>> sortedValues,
            Property property) {
        Property min = model.createProperty(RDF_MIN);
        Property max = model.createProperty(RDF_MAX);
        return IntStream.range(0, nrOfBuckets)
                .mapToObj(position -> {
                    List<T> range = getBucketRange(sortedValues, position, nrOfBuckets);
                    Resource generalizationResource = model.createResource(property.getURI() + "_" + position);
                    generalizationResource.addProperty(RDF.type, QueryService.SOYA_URL + "generalization");
                    if (position != 0) {
                        generalizationResource.addLiteral(min, range.get(0));
                    } else {
                        generalizationResource.addProperty(
                                RDFS.comment,
                                "For the lower bound the minimum value is obfuscated");
                    }
                    if (position != nrOfBuckets - 1) {
                        generalizationResource.addLiteral(max, range.get(1));
                    } else {
                        generalizationResource.addProperty(
                                RDFS.comment,
                                "For the higher bound the maximum value is obfuscated");
                    }
                    return generalizationResource;
                }).toList();
    }

    protected List<T> getBucketRange(List<Pair<Resource, T>> sortedValues, int bucketNumber, int nrOfBuckets) {
        int lowerBoundIndex = bucketNumber * sortedValues.size() / nrOfBuckets;
        T lowerBound = getMedianValue(
                lowerBoundIndex > 0 ? sortedValues.get(lowerBoundIndex - 1).getRight() : null,
                sortedValues.get(lowerBoundIndex).getRight());
        int upperBoundIndex = ((bucketNumber + 1) * sortedValues.size() / nrOfBuckets) - 1;
        T upperBound = getMedianValue(
                sortedValues.get(upperBoundIndex).getRight(),
                upperBoundIndex + 1 < sortedValues.size() ? sortedValues.get(upperBoundIndex + 1).getRight() : null);
        return List.of(lowerBound, upperBound);
    }
}
