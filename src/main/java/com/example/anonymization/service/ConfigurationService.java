package com.example.anonymization.service;

import com.example.anonymization.entities.Configuration;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
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

    public static Map<Resource, Map<Property, Configuration>> fetchConfig(String url) {
        String configString = fetchStringContent(url);
        Model configModel = ModelFactory.createDefaultModel();
        RDFParser.create()
                .source(new StringReader(configString))
                .lang(Lang.JSONLD)
                .parse(configModel);
        return extractConfig(configModel);
    }

    private static String fetchStringContent(String url) {
        // TODO proper exception handling
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 300) {
                return response.body();
            } else {
                throw new IllegalArgumentException("");
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            System.out.println("Exception when fetching the URL content");
            throw new IllegalArgumentException("Exception when fetching the URL content");
        }
    }

    private static Map<Resource, Map<Property, Configuration>> extractConfig(Model model) {
        String query = """
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                    PREFIX soya: <https://w3id.org/soya/ns#>
                    SELECT ?anonymizationObject ?attribute ?datatype ?anonymization WHERE {
                      ?overlay a soya:OverlayClassification .
                      ?overlay soya:onBase ?anonymizationObject .
                      ?attribute rdfs:domain ?anonymizationObject .
                      ?attribute rdfs:range ?datatype .
                      ?attribute <https://w3id.org/soya/ns#classification> ?anonymization .
                    }
                """;
        Map<Resource, Map<Property, Configuration>> configs = new HashMap<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = qexec.execSelect();
            while(resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                Resource anonymizationObject = solution.get("anonymizationObject").asResource();
                if (!configs.containsKey(anonymizationObject)) {
                    configs.put(anonymizationObject, new HashMap<>());
                }
                configs.get(anonymizationObject).put(
                        ResourceFactory.createProperty(solution.get("attribute").asResource().getURI()),
                        new Configuration(
                            extractValueFromURL(solution.get("datatype").toString()),
                            extractValueFromURL(solution.get("anonymization").toString())
                        )
                );
            }
        }
        return configs;
    }

    public static String extractValueFromURL(String url) {
        int lastIndex = Math.max(url.lastIndexOf('/'), url.lastIndexOf('#'));
        return (lastIndex != -1) ? url.substring(lastIndex + 1) : url;
    }
}
