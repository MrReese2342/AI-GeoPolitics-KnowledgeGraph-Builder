package com.geokg.backend.service;

import com.geokg.backend.ai.OllamaClient;
import com.geokg.backend.ai.dto.ExtractionResult;
import com.geokg.backend.domain.Article;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class ArticleAnalysisService {

    private final OllamaClient ollama;
    private final GraphWriterService writer;

    public ArticleAnalysisService(OllamaClient ollama, GraphWriterService writer) {
        this.ollama = ollama;
        this.writer = writer;
    }

    public ExtractionResult analyzeAndPersist(Article article) {
        // fallback date = date ingestion (v1)
        LocalDate fallback = article.getIngestedAt().atZone(ZoneOffset.UTC).toLocalDate();

        ExtractionResult result = ollama.extractConflictSignals(
                article.getText(),
                fallback.toString()
        );

        writer.writeExtraction(article.getId(), result, fallback);
        return result;
    }
}