package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.data.QueryService;
import com.example.anonymization.entities.ObjectGeneralizationConfig;
import com.example.anonymization.exceptions.OntologyException;
import jakarta.validation.constraints.NotNull;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class ConfigurationService {

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    /**
     * Fetches the configuration from the given URL and extracts it into a map.
     * @param url The URL to fetch the configuration from.
     * @return A map where the key is the Resource (object) and the value is another map
     * mapping Properties to their Configurations.
     */
    @NotNull
    public static Map<Resource, Map<Property, Configuration>> fetchConfigForObjects(String url) {
        return extractConfig(getModel(url));
    }

    /**
     * Creates a flat configuration from anonymization objects
     * @param configs The map of anonymization objects to their configurations
     * @return A flat map where the key is the Property and the value is its Configuration.
     */
    @NotNull
    public static Map<Property, Configuration> createFlatConfig(Map<Resource, Map<Property, Configuration>> configs) {
         Map<Property, Configuration> flatConfig = new HashMap<>();
         for (Map<Property, Configuration> configMap : configs.values()) {
             flatConfig.putAll(configMap);
         }
         return flatConfig;
    }

    @NotNull
    private static Model getModel(String url) {
        logger.info("Fetching config from url: {}", url);
        String configString = fetchStringContent(url);
        logger.info("Config successfully fetched");
        try {
            Model configModel = ModelFactory.createDefaultModel();
            RDFParser.create()
                    .source(new StringReader(configString))
                    .lang(Lang.JSONLD)
                    .parse(configModel);
            return configModel;
        } catch (Exception e) {
            throw new OntologyException("Exception when parsing the fetched ontology");
        }
    }

    @NotNull
    private static String fetchStringContent(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                String body = response.body();
                if (body == null || body.isEmpty()) {
                    throw new OntologyException(
                            "Successful request but empty response body for fetching ontology from: " + url
                    );
                }
                return body;
            } else {
                throw new OntologyException(
                        String.format("Failed to fetch ontology from URL: %s. HTTP status: %d", url, status)
                );
            }
        } catch (URISyntaxException | IOException e) {
            throw new OntologyException("Failed to fetch ontology from URL: " + url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore thread interruption status
            throw new OntologyException("Thread was interrupted while fetching ontology from URL: " + url);
        }
    }

    @NotNull
    private static Map<Resource, Map<Property, Configuration>> extractConfig(Model model) {
        Map<Resource, Map<Property, Configuration>> configs = new HashMap<>();
        Set<Property> properties = new HashSet<>();
        logger.info("Extracting configuration from server response");
        QueryService.getConfigurations(model).forEach(entry -> {
            if (properties.contains(entry.property())) {
                throw new OntologyException(
                        "Duplicate Property key found in Ontology: " + entry.property().toString()
                );
            }
            properties.add(entry.property());
            if (!configs.containsKey(entry.object())) {
                configs.put(entry.object(), new HashMap<>());
            }
            configs.get(entry.object()).put(
                    entry.property(),
                    createConfiguration(entry.datatype(), entry.anonymization(), entry.property(), model)
            );
            logger.info(
                    "New Config: {}, {}, {}",
                    extractValueFromURL(entry.property().toString()),
                    extractValueFromURL(entry.datatype().toString()),
                    extractValueFromURL(entry.anonymization().toString())
            );
        });
        logger.info("Configuration successfully converted");
        return configs;
    }

    private static Configuration createConfiguration(
            Resource datatype,
            Literal anonymization,
            Property property,
            Model model
    ) {
        String datatypeString = extractValueFromURL(datatype.toString());
        String anonymizationString = extractValueFromURL(anonymization.toString());
        if (anonymizationString.equals("generalization") && !Set.of("integer", "double", "date").contains(datatypeString)) {
            return new ObjectGeneralizationConfig(
                    datatypeString,
                    QueryService.getAttributeOrder(model, property)
            );
        } else {
            return new Configuration(datatypeString, anonymizationString);
        }
    }

    public static String extractValueFromURL(String url) {
        int lastIndex = Math.max(url.lastIndexOf('/'), url.lastIndexOf('#'));
        return (lastIndex != -1) ? url.substring(lastIndex + 1) : url;
    }
}
