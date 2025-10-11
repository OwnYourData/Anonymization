package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import com.example.anonymization.data.QueryService;
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

    public static Map<Resource, Map<Property, Configuration>> fetchConfigForObjects(String url) {
        return extractConfig(getModel(url));
    }

    public static Map<Property, Configuration> fetchFlatConfig(String url) {
         Map<Resource, Map<Property, Configuration>> configs = fetchConfigForObjects(url);
         Map<Property, Configuration> flatConfig = new HashMap<>();
         for (Map<Property, Configuration> configMap : configs.values()) {
             for (Map.Entry<Property, Configuration> entry : configMap.entrySet()) {
                 if (flatConfig.containsKey(entry.getKey())) {
                     throw new IllegalStateException("Duplicate Property key found: " + entry.getKey());
                 }
                 flatConfig.put(entry.getKey(), entry.getValue());
             }
         }
         return flatConfig;
    }

    private static Model getModel(String url) {
        logger.info("Fetching config from url: {}", url);
        String configString = fetchStringContent(url);
        logger.info("Config successfully fetched");
        Model configModel = ModelFactory.createDefaultModel();
        RDFParser.create()
                .source(new StringReader(configString))
                .lang(Lang.JSONLD)
                .parse(configModel);
        return configModel;
    }

    private static String fetchStringContent(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 300) {
                return response.body();
            } else {
                throw new IllegalArgumentException("Exception when fetching the URL content");
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error("Exception when fetching the URL content", e);
            throw new IllegalArgumentException("Exception when fetching the URL content");
        }
    }

    private static Map<Resource, Map<Property, Configuration>> extractConfig(Model model) {
        Map<Resource, Map<Property, Configuration>> configs = new HashMap<>();
        logger.info("Extracting configuration from server response");
        QueryService.getConfigurations(model).forEach(entry -> {
            if (!configs.containsKey(entry.object())) {
                configs.put(entry.object(), new HashMap<>());
            }
            configs.get(entry.object()).put(
                    entry.attribute(),
                    new Configuration(
                            extractValueFromURL(entry.datatype().toString()),
                            extractValueFromURL(entry.anonymization().toString())
                    )
            );
            logger.info(
                    "New Config: {}, {}, {}",
                    extractValueFromURL(entry.attribute().toString()),
                    extractValueFromURL(entry.datatype().toString()),
                    extractValueFromURL(entry.anonymization().toString())
            );
        });
        logger.info("Configuration successfully converted");
        return configs;
    }

    public static String extractValueFromURL(String url) {
        int lastIndex = Math.max(url.lastIndexOf('/'), url.lastIndexOf('#'));
        return (lastIndex != -1) ? url.substring(lastIndex + 1) : url;
    }
}
