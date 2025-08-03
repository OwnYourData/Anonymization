package com.example.anonymization.service.anonymizer;


import com.example.anonymization.service.OntologyService;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.jena.rdfs.assembler.VocabRDFS.NS;

public abstract class Generalization<T> implements Anonymization {

    // TODO include number of buckets used

    @Override
    public void applyAnoynmization(Model model, Property property, Map<Resource, Literal> data) {
        List<Pair<Resource, T>> sortedValues = getSortedValues(data);
        Map<Resource, List<T>> ranges = getRanges(sortedValues);
        writeToModel(model, ranges, property);
    }

    protected abstract List<Pair<Resource, T>> getSortedValues(Map<Resource, Literal> data);

    protected Map<Resource, List<T>> getRanges(List<Pair<Resource, T>> sortedValues) {
        List<Pair<Resource, Integer>> positionValues = new LinkedList<>();
        for (int i = 0; i < sortedValues.size(); i++) {
            positionValues.add(new Pair<>(
                    sortedValues.get(i).getLeft(),
                    3 * i / sortedValues.size())
            );
        }
        return positionValues.stream()
                .map(e -> new Pair<>(e.getLeft(), getBucketRange(sortedValues, e.getRight())))
                .collect(Collectors.toMap(
                        Pair::getLeft,
                        Pair::getRight
                ));
    }

    protected void writeToModel(Model model, Map<Resource, List<T>> data, Property property) {
        Property min = model.createProperty(NS, "min");
        Property max = model.createProperty(NS, "max");

        data.forEach((key, value) -> {
            Resource generalizationResource = model.createResource(key.getURI() + property.getLocalName());
            generalizationResource.addProperty(RDF.type, OntologyService.SOYA_URL + "generalization");
            key.addProperty(property, generalizationResource);
            generalizationResource.addLiteral(min, value.get(0));
            generalizationResource.addLiteral(max, value.get(1));
        });
    }

    protected List<T> getBucketRange(List<Pair<Resource, T>> sortedValues, int bucketNumber) {
        return List.of(
            sortedValues.get(bucketNumber * 3).getRight(),
            sortedValues.get(((bucketNumber + 1) * 3) - 1).getRight()
        );
    }}
