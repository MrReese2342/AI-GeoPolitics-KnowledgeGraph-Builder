package com.geokg.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.geokg.backend.ai.dto.ExtractionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class OllamaClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    public ExtractionResult extractConflictSignals(String articleText, String publishedDateIso) {
        try {
            String prompt = buildPrompt(articleText, publishedDateIso);
            JsonNode schema = buildSchema();

            // IMPORTANT: body doit être un ObjectNode (pas JsonNode),
            // sinon on ne peux pas appeler .set(...)
            ObjectNode body = om.createObjectNode();
            body.put("model", model);
            body.put("stream", false);

            ArrayNode messages = om.createArrayNode();
            messages.add(
                    om.createObjectNode()
                            .put("role", "user")
                            .put("content", prompt)
            );

            body.set("messages", messages);
            body.set("format", schema);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new IllegalStateException("Ollama error HTTP " + res.statusCode() + ": " + res.body());
            }

            JsonNode root = om.readTree(res.body());
            String content = root.path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                return ExtractionResult.empty();
            }

            // content est censé être du JSON conforme au schema
            return om.readValue(content, ExtractionResult.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract conflict signals from Ollama", e);
        }
    }

    private String buildPrompt(String articleText, String publishedDateIso) {
        return """
                You are an information extraction system for geopolitical conflict detection.
                RULES:
                - Only use facts present in the article text. Do NOT invent.
                - If an item is uncertain, omit it or set a low confidence (<= 0.4).
                - Evidence MUST be a short excerpt from the text (max ~25 words).
                - Use ISO date format YYYY-MM-DD. If unknown, use null.
                - Keep actor names short and canonical (e.g., "United States" not "Washington").

                OUTPUT must be valid JSON matching the schema.

                Context:
                - Article published date (may help infer occurredAt if explicitly implied): %s

                Article text:
                %s
                """.formatted(publishedDateIso, articleText);
    }

    private JsonNode buildSchema() {
        ObjectNode obj = om.createObjectNode();
        obj.put("type", "object");

        ObjectNode props = om.createObjectNode();
        obj.set("properties", props);

        // actors
        ObjectNode actors = om.createObjectNode();
        actors.put("type", "array");

        ObjectNode actorItem = om.createObjectNode();
        actorItem.put("type", "object");

        ObjectNode actorProps = om.createObjectNode();
        actorProps.set("name", om.createObjectNode().put("type", "string"));
        actorProps.set("type", om.createObjectNode()
                .put("type", "string")
                .set("enum", om.createArrayNode().add("COUNTRY").add("ORG").add("PERSON")));

        actorItem.set("properties", actorProps);
        actorItem.set("required", om.createArrayNode().add("name").add("type"));
        actors.set("items", actorItem);

        props.set("actors", actors);

        // events
        ObjectNode events = om.createObjectNode();
        events.put("type", "array");

        ObjectNode eventItem = om.createObjectNode();
        eventItem.put("type", "object");

        ObjectNode eventProps = om.createObjectNode();
        eventProps.set("type", om.createObjectNode().put("type", "string"));
        eventProps.set("occurredAt", om.createObjectNode().put("type", "string"));
        eventProps.set("summary", om.createObjectNode().put("type", "string"));
        eventProps.set("confidence", om.createObjectNode().put("type", "number"));

        eventItem.set("properties", eventProps);
        eventItem.set("required", om.createArrayNode().add("type").add("summary").add("confidence"));
        events.set("items", eventItem);

        props.set("events", events);

        // interactions
        ObjectNode interactions = om.createObjectNode();
        interactions.put("type", "array");

        ObjectNode interItem = om.createObjectNode();
        interItem.put("type", "object");

        ObjectNode interProps = om.createObjectNode();
        interProps.set("from", om.createObjectNode().put("type", "string"));
        interProps.set("to", om.createObjectNode().put("type", "string"));
        interProps.set("type", om.createObjectNode().put("type", "string"));
        interProps.set("polarity", om.createObjectNode().put("type", "integer"));
        interProps.set("weight", om.createObjectNode().put("type", "integer"));
        interProps.set("occurredAt", om.createObjectNode().put("type", "string"));
        interProps.set("evidence", om.createObjectNode().put("type", "string"));
        interProps.set("confidence", om.createObjectNode().put("type", "number"));

        interItem.set("properties", interProps);
        interItem.set("required", om.createArrayNode()
                .add("from").add("to").add("type")
                .add("polarity").add("weight")
                .add("evidence").add("confidence"));

        interactions.set("items", interItem);

        props.set("interactions", interactions);

        obj.set("required", om.createArrayNode().add("actors").add("events").add("interactions"));
        return obj;
    }
}