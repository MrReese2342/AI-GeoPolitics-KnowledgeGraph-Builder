package com.geokg.backend.service;

import com.geokg.backend.ai.dto.ActorDto;
import com.geokg.backend.ai.dto.EventDto;
import com.geokg.backend.ai.dto.ExtractionResult;
import com.geokg.backend.ai.dto.InteractionDto;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class GraphWriterService {

    private final Neo4jClient neo4j;

    public GraphWriterService(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    @Transactional
    public void writeExtraction(String articleId, ExtractionResult result, LocalDate fallbackDate) {

        // 1) Actors + MENTIONS
        for (ActorDto a : result.actors()) {
            String name = canon(a.name());
            String type = a.type() == null ? "ORG" : a.type().toUpperCase(Locale.ROOT);

            neo4j.query("""
                    MERGE (x:Actor {name: $name})
                    SET x.type = $type
                    WITH x
                    MATCH (art:Article {id: $articleId})
                    MERGE (art)-[:MENTIONS]->(x)
                    """)
                    .bind(name).to("name")
                    .bind(type).to("type")
                    .bind(articleId).to("articleId")
                    .run();
        }

        // 2) Events + DESCRIBES
        for (EventDto e : result.events()) {
            String eventId = UUID.randomUUID().toString();
            LocalDate occurredAt = parseDateOr(e.occurredAt(), fallbackDate);
            double conf = e.confidence() == null ? 0.5 : e.confidence();

            neo4j.query("""
                    MATCH (art:Article {id: $articleId})
                    CREATE (ev:Event {
                      id: $eventId,
                      type: $type,
                      occurredAt: date($occurredAt),
                      summary: $summary,
                      confidence: $confidence
                    })
                    MERGE (art)-[:DESCRIBES]->(ev)
                    """)
                    .bind(articleId).to("articleId")
                    .bind(eventId).to("eventId")
                    .bind(safe(e.type())).to("type")
                    .bind(occurredAt.toString()).to("occurredAt")
                    .bind(safe(e.summary())).to("summary")
                    .bind(conf).to("confidence")
                    .run();
        }

        // 3) Interactions Actor->Actor (le coeur “conflit”)
        for (InteractionDto it : result.interactions()) {
            String relId = UUID.randomUUID().toString();
            String from = canon(it.from());
            String to = canon(it.to());
            LocalDate occurredAt = parseDateOr(it.occurredAt(), fallbackDate);

            int polarity = (it.polarity() == null || it.polarity() == 0) ? -1 : it.polarity();

            int weight = (it.weight() == null || it.weight() < 1) ? 1 : it.weight();
            if (weight > 5) weight = 5;
            double conf = it.confidence() == null ? 0.5 : it.confidence();

            neo4j.query("""
                    MERGE (a:Actor {name:$from})
                    MERGE (b:Actor {name:$to})
                    WITH a,b
                    CREATE (a)-[:INTERACTION {
                      id: $relId,
                      type: $type,
                      polarity: $polarity,
                      weight: $weight,
                      occurredAt: date($occurredAt),
                      articleId: $articleId,
                      evidence: $evidence,
                      confidence: $confidence
                    }]->(b)
                    """)
                    .bind(from).to("from")
                    .bind(to).to("to")
                    .bind(relId).to("relId")
                    .bind(safe(it.type())).to("type")
                    .bind(polarity).to("polarity")
                    .bind(weight).to("weight")
                    .bind(occurredAt.toString()).to("occurredAt")
                    .bind(articleId).to("articleId")
                    .bind(safe(it.evidence())).to("evidence")
                    .bind(conf).to("confidence")
                    .run();
        }
    }

    private static String canon(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static LocalDate parseDateOr(String iso, LocalDate fallback) {
        try {
            if (iso == null || iso.isBlank()) return fallback;
            return LocalDate.parse(iso.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}