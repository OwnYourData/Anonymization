package com.example.anonymization.service.anonymizer;


import com.example.anonymization.service.KpiService;
import com.example.anonymization.data.QueryService;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class Generalization<T> implements Anonymization {

    public static final String RDF_MAX = "http://www.w3.org/2000/01/rdf-schema#max";
    public static final String RDF_MIN = "http://www.w3.org/2000/01/rdf-schema#min";

    @Override
    public void applyAnonymization(Model model, Property property, Map<Resource, Literal> data, long numberAttributes) {
        int numberBuckets = Anonymization.calculateNumberOfBuckets(data.size(), numberAttributes);
        KpiService.addNrBuckets(model, property, numberBuckets);
        List<Pair<Resource, T>> sortedValues = getSortedValues(data);
        List<Resource> buckets = createBuckets(model, numberBuckets, sortedValues, property);
        Map<Resource, Resource> ranges = getRanges(sortedValues, numberBuckets, buckets);
        writeToModel(model, ranges, property);
    }

    protected abstract List<Pair<Resource, T>> getSortedValues(Map<Resource, Literal> data);

    protected Map<Resource, Resource> getRanges(List<Pair<Resource, T>> sortedValues, int numberBuckets, List<Resource> buckets) {
        List<Pair<Resource, Integer>> positionValues = new LinkedList<>();
        for (int i = 0; i < sortedValues.size(); i++) {
            positionValues.add(new Pair<>(
                    sortedValues.get(i).getLeft(),
                    numberBuckets * i / sortedValues.size())
            );
        }
        return positionValues.stream()
                .map(e -> new Pair<>(e.getLeft(), buckets.get(e.getRight())))
                .collect(Collectors.toMap(
                        Pair::getLeft,
                        Pair::getRight
                ));
    }

    protected void writeToModel(Model model, Map<Resource, Resource> data, Property property) {
        Property generalized = model.createProperty(property.getURI(), "_generalized");
        data.forEach((key, value) -> key.addProperty(generalized, value));
    }

    protected List<Resource> createBuckets(Model model, int nrOfBuckets, List<Pair<Resource, T>> sortedValues, Property property) {
        Property min = model.createProperty(RDF_MIN);
        Property max = model.createProperty(RDF_MAX);
        return IntStream.range(0, nrOfBuckets)
                .mapToObj(position -> {
                    List<T> range = getBucketRange(sortedValues, position, nrOfBuckets);
                    Resource generalizationResource = model.createResource(property.getURI() + "_" + position);
                    generalizationResource.addProperty(RDF.type, QueryService.SOYA_URL + "generalization");
                    generalizationResource.addLiteral(min, range.get(0));
                    generalizationResource.addLiteral(max, range.get(1));
                    return generalizationResource;
                }).toList();
    }

    protected List<T> getBucketRange(List<Pair<Resource, T>> sortedValues, int bucketNumber, int nrOfBuckets) {
        return List.of(
            sortedValues.get(bucketNumber * sortedValues.size() / nrOfBuckets).getRight(),
            sortedValues.get(((bucketNumber + 1) * sortedValues.size() / nrOfBuckets) - 1).getRight()
        );
    }
}
