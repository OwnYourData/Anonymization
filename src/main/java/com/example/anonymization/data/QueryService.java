package com.example.anonymization.data;

import com.example.anonymization.exceptions.AnonymizationException;
import com.example.anonymization.exceptions.RequestModelException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.anonymization.service.AnonymizationService.FLAT_OBJECT_NAME;
import static com.example.anonymization.service.KpiService.*;

@Service
public class QueryService {

    public static final String SOYA_URL = "http://ns.ownyourdata.eu/ns/soya-context/";

    /**
     * Fetches the configuration from an input model
     * @param model the input model
     * @return configuration representation with object, property, datatype and anonymization type
     */
    public static List<ConfigurationResult> getConfigurations(Model model) {
        List<ConfigurationResult> configurations = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(QueryBuildingService.createConfigQuery(), model)) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution solution = rs.nextSolution();
                configurations.add(new  ConfigurationResult(
                        solution.getResource("?anonymizationObject"),
                        ResourceFactory.createProperty(solution.getResource("?property").getURI()),
                        solution.getResource("?datatype"),
                        solution.getLiteral("?anonymization")
                ));
            }
        }
        return  configurations;
    }

    /**
     * Extracts the data for a given set of attributes and an object type
     * @param model the input model
     * @param properties attributes for which data should be fetched
     * @param objectType the type for which data should be fetched
     * @return mapping of resources of the object type with their property data
     */
    public static Map<Resource, Map<Property, Literal>> getData(
            Model model,
            Collection<Property> properties,
            Collection<Resource> objectType
    ) {
        Query query = QueryBuildingService.createDataModelQuery(properties, objectType).asQuery();
        Map<Resource, Map<Property, Literal>> results = new HashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                Map<Property, Literal> propertyValues = new HashMap<>();
                properties.forEach(property -> {
                    Literal value = solution.getLiteral("_" + property.getLocalName());
                    if (value != null) {
                        propertyValues.put(property, value);
                    }
                });
                results.put(solution.getResource("object"), propertyValues);
            }
        } catch (Exception ex) {
            throw new RequestModelException("Error during fetching data for anonymization: " + ex.getMessage());
        }
        return results;
    }

    /**
     * Extracts the data for a given set of attributes and an object type
     * @param model the input model
     * @param properties attributes for which data should be fetched
     * @return mapping of resources of the object type with their property data
     */
    public static Map<Property, Literal> getDataKpi(Model model, Set<Property> properties) {
        Query query = QueryBuildingService.createKpiDataQuery(
                properties,
                model.getResource(SOYA_URL + "kpiObjectAnonymizationDemo2") // TODO query for the right url (kpiObject + local url of resource)
        ).asQuery();
        Map<Property, Literal> results = new HashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            QuerySolution solution = resultSet.nextSolution();
            properties.forEach(property -> {
                Literal value = solution.getLiteral("_" + property.getLocalName());
                if (value != null) {
                    results.put(property, value);
                }
            });
        } catch (Exception ex) {
            throw new AnonymizationException("Error during fetching KPI data: " + ex.getMessage());
        }
        return results;
    }


    /**
     * Extracts the parameters to with anonymization should be applied --> config is defined and the property is used
     * at least for one object
     * @param model the input model
     * @param configs list of configurations
     * @param objectType definition of the object type to which anonymization is applied
     */
    public static Set<Property> getProperties(Model model, Set<Property> configs, Resource objectType) {
        Query query = QueryBuildingService.createPropertyQuery(configs, objectType).asQuery();
        Set<Property> properties = new HashSet<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                if (solution.getLiteral("used").getBoolean()) {
                    properties.add(model.getProperty(solution.getResource("predicate").getURI()));
                }
            }
        } catch (Exception e) {
            throw new RequestModelException("Error during fetching properties for anonymization: " + e.getMessage());
        }
        return properties;
    }

    /**
     * Deletes the original values that are now replaced with anonymized values
     * @param model the input model
     * @param properties the properties that are removed
     * @param objectType the object type for which the data is removed
     */
    public static void deleteOriginalProperties(Model model, Set<Property> properties, Resource objectType) {
        try {
            UpdateRequest updateRequest = QueryBuildingService.deleteOriginalPropertyQuery(properties, objectType).asUpdate();
            UpdateAction.execute(updateRequest, model);
        } catch (Exception ex) {
            throw new AnonymizationException("Error during deleting original properties: " + ex.getMessage());
        }
    }

    /**
     * Extracts the randomized properties and the original values
     * @param model the input model
     * @param anonymizationObject the object type for which the data is returned
     * @param property the randomization property
     * @return randomization representation as a list of objects with their randomized and original properties
     */
    public static List<RandomizationResult> getRandomizationResults(Model model, Resource anonymizationObject, Property property) {
        Query query = QueryBuildingService.createRandomizationQuery(model, anonymizationObject, property).asQuery();
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
        } catch (Exception ex) {
            throw new AnonymizationException("Error during fetching randomization results: " + ex.getMessage());
        }
        return results;
    }

    /**
     * Extracts a group representation of the generalization result
     * @param model the input model
     * @param anonymizationObject the object type for which the data is returned
     * @param properties set of properties by which the result is grouped
     * @return groups of resources with the same property values
     */
    public static List<Set<Resource>> getGeneralizationGroups(Model model, Resource anonymizationObject, Set<Property> properties) {
        ParameterizedSparqlString queryString = QueryBuildingService.createGroupsQuery(properties, anonymizationObject);
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
        } catch (Exception ex) {
            throw new AnonymizationException("Error during fetching generalization groups: " + ex.getMessage());
        }
        return results;
    }

    public static Map<Resource, Map<Property, Literal>> getAllData(Model model, Collection<Resource> objectType) {
        Set<Property> properties = new HashSet<>();
        ParameterizedSparqlString pss = QueryBuildingService.createPropertyQuery(objectType);
        try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), model)) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution solution = rs.nextSolution();
                properties.add(ResourceFactory.createProperty(solution.getResource("?predicate").getURI()));
            }
        }
        return getData(model, properties, objectType);
    }

    public static Map<Resource, Map<Property, List<Literal>>> getGeneralizationData(
            Model model,
            Collection<Resource> objectType,
            Set<Property> properties
    ) {
        model.write(System.out);
        ParameterizedSparqlString queryString = QueryBuildingService.createGeneralizationData(properties, objectType);
        Query query = queryString.asQuery();
        Map<Resource, Map<Property, List<Literal>>> results = new HashMap<>();
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                Map<Property, List<Literal>> values = new HashMap<>();
                properties.forEach(property -> {
                    Literal minValue = sol.getLiteral("_min_" + property.getLocalName());
                    Literal maxValue = sol.getLiteral("_max_" + property.getLocalName());
                    if (minValue != null  && maxValue != null) {
                        values.put(property, List.of(minValue, maxValue));
                    }
                });
                results.put(sol.getResource("object"), values);
            }
        } catch (Exception ex) {
            throw new AnonymizationException("Error during fetching generalization data: " + ex.getMessage());
        }
        return results;
    }

    public static Long getKAnonymity(Model model) {
        ParameterizedSparqlString queryString = QueryBuildingService.createKAnonymityQuery(
                model.createResource(KPI_OBJECT_URI + FLAT_OBJECT_NAME),
                model.createProperty(K_ANONYMITY)
        );
        Query query = queryString.asQuery();
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            if (rs.hasNext()) {
                QuerySolution sol = rs.next();
                return sol.getLiteral("value").getLong();
            }
        }
        return null;
    }

    public static List<AttributeInformation> getAttributeInformation(Model model) {
        ParameterizedSparqlString queryString = QueryBuildingService.createAttributeInformationQuery(
                model.createResource(KPI_OBJECT_URI + FLAT_OBJECT_NAME),
                model.createProperty(HAS_ATTRIBUTE_URI),
                model.createProperty(NR_ATTRIBUTES_URI),
                model.createProperty(ANONYMIZATION_TYP_URI)
        );
        Query query = queryString.asQuery();
        List<AttributeInformation> results = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                Long nrBuckets = sol.getLiteral("nrBuckets") == null ?
                        null : sol.getLiteral("nrBuckets").getLong();
                results.add(new AttributeInformation(
                        sol.getResource("attribute"),
                        sol.getLiteral("anonymization").toString(),
                        nrBuckets
                ));
            }
        }
        return results;
    }


    public record ConfigurationResult(Resource object, Property property, Resource datatype, Literal anonymization) {}
    public record RandomizationResult(Resource object, Literal original, Literal randomized) {}
    public record AttributeInformation(Resource attribute, String anonymization, Long nrBuckets) {}
}
