package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.data.QueryService;
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
import java.util.HashMap;
import java.util.Map;

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
     * Fetches the configuration from the given URL and extracts it into a flat map.
     * @param url The URL to fetch the configuration from.
     * @return A flat map where the key is the Property and the value is its Configuration.
     */
    @NotNull
    public static Map<Property, Configuration> fetchFlatConfig(String url) {
         Map<Resource, Map<Property, Configuration>> configs = fetchConfigForObjects(url);
         Map<Property, Configuration> flatConfig = new HashMap<>();
         for (Map<Property, Configuration> configMap : configs.values()) {
             for (Map.Entry<Property, Configuration> entry : configMap.entrySet()) {
                 if (flatConfig.containsKey(entry.getKey())) {
                     throw new OntologyException("Duplicate Property key found in Flat Ontology: " + entry.getKey());
                 }
                 flatConfig.put(entry.getKey(), entry.getValue());
             }
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
            if (response.statusCode() < 300 || response.body().isEmpty()) {
                return response.body();
            } else {
                throw new OntologyException("Exception when fetching the URL content from the provided URL: " + url);
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new IllegalArgumentException("Exception when fetching the URL content from the provided URL: " + url);
        }
    }

    @NotNull
    private static Map<Resource, Map<Property, Configuration>> extractConfig(Model model) {
        Map<Resource, Map<Property, Configuration>> configs = new HashMap<>();
        logger.info("Extracting configuration from server response");
        try {
            QueryService.getConfigurations(model).forEach(entry -> {
                if (!configs.containsKey(entry.object())) {
                    configs.put(entry.object(), new HashMap<>());
                }
                configs.get(entry.object()).put(
                        entry.property(),
                        new Configuration(
                                extractValueFromURL(entry.datatype().toString()),
                                extractValueFromURL(entry.anonymization().toString())
                        )
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
        } catch (Exception e) {
            throw new OntologyException("Exception when extracting configuration from the fetched ontology");
        }
    }

    public static String extractValueFromURL(String url) {
        int lastIndex = Math.max(url.lastIndexOf('/'), url.lastIndexOf('#'));
        return (lastIndex != -1) ? url.substring(lastIndex + 1) : url;
    }
}
