package com.example.anonymization.service.anonymizer;


import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.jena.rdfs.assembler.VocabRDFS.NS;

public abstract class Generalization<T> implements Anonymization {

    protected Map<Resource, T[]> getRanges(List<Pair<Resource, T>> sortedValues, T[] sortedArray) {
        List<Pair<Resource, Integer>> positionValues = new LinkedList<>();
        for (int i = 0; i < sortedValues.size(); i++) {
            positionValues.add(new Pair<>(
                    sortedValues.get(i).getLeft(),
                    3 * i / sortedValues.size())
            );
        }
        return positionValues.stream()
                .map(e -> new Pair<>(e.getLeft(), getBucketRange(sortedArray, e.getRight())))
                .collect(Collectors.toMap(
                        Pair::getLeft,
                        Pair::getRight
                ));
    }

    protected void writeToModel(Model model, Map<Resource, T[]> data, Property property) {
        Property min = model.createProperty(NS, "min");
        Property max = model.createProperty(NS, "max");

        data.forEach((key, value) -> {
            // TODO define a datatype for the objects
            Resource generalizationResource = model.createResource(key.getURI() + property.getLocalName());
            key.addProperty(property, generalizationResource);
            generalizationResource.addLiteral(min, value[0]);
            generalizationResource.addLiteral(max, value[1]);
        });
    }

    protected abstract T[] getBucketRange(T[] sortedValues, int bucketNumber);
}
