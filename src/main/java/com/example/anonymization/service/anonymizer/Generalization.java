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

    @Override
    public void applyAnonymization(Model model, Property property, Map<Resource, Literal> data, long numberAttributes) {
        int numberBuckets = Anonymization.calculateNumberOfBuckets(data.size(), numberAttributes);
        List<Pair<Resource, T>> sortedValues = getSortedValues(data);
        Map<Resource, List<T>> ranges = getRanges(sortedValues, numberBuckets);
        writeToModel(model, ranges, property);
    }

    protected abstract List<Pair<Resource, T>> getSortedValues(Map<Resource, Literal> data);

    protected Map<Resource, List<T>> getRanges(List<Pair<Resource, T>> sortedValues, int numberBuckets) {
        List<Pair<Resource, Integer>> positionValues = new LinkedList<>();
        for (int i = 0; i < sortedValues.size(); i++) {
            positionValues.add(new Pair<>(
                    sortedValues.get(i).getLeft(),
                    numberBuckets * i / sortedValues.size())
            );
        }
        return positionValues.stream()
                .map(e -> new Pair<>(e.getLeft(), getBucketRange(sortedValues, e.getRight(), numberBuckets)))
                .collect(Collectors.toMap(
                        Pair::getLeft,
                        Pair::getRight
                ));
    }

    protected void writeToModel(Model model, Map<Resource, List<T>> data, Property property) {
        Property min = model.createProperty(NS, "min");
        Property max = model.createProperty(NS, "max");
        Property generalizaed = model.createProperty(property.getURI(), "_generalized");

        data.forEach((key, value) -> {
            Resource generalizationResource = model.createResource(property.getURI() + "_" + key.getLocalName());
            generalizationResource.addProperty(RDF.type, OntologyService.SOYA_URL + "generalization");
            key.addProperty(generalizaed, generalizationResource);
            generalizationResource.addLiteral(min, value.get(0));
            generalizationResource.addLiteral(max, value.get(1));
        });
    }

    protected List<T> getBucketRange(List<Pair<Resource, T>> sortedValues, int bucketNumber, int nrOfBuckets) {
        return List.of(
            sortedValues.get(bucketNumber * nrOfBuckets).getRight(),
            sortedValues.get(((bucketNumber + 1) * nrOfBuckets) - 1).getRight()
        );
    }
}
