package com.geokg.backend.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.Instant;

@Node("Article")
public class Article {

    @Id
    private String id;

    private String title;
    private String source;
    private String text;

    private Instant ingestedAt;

    public Article() {}

    public Article(String id, String title, String source, String text, Instant ingestedAt) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.text = text;
        this.ingestedAt = ingestedAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSource() { return source; }
    public String getText() { return text; }
    public Instant getIngestedAt() { return ingestedAt; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setSource(String source) { this.source = source; }
    public void setText(String text) { this.text = text; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
}