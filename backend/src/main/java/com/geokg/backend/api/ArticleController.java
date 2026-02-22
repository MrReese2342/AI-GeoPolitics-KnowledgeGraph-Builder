package com.geokg.backend.api;

import com.geokg.backend.api.dto.CreateArticleRequest;
import com.geokg.backend.domain.Article;
import com.geokg.backend.repo.ArticleRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.geokg.backend.ai.dto.ExtractionResult;
import com.geokg.backend.service.ArticleAnalysisService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final ArticleAnalysisService analysisService;

    public ArticleController(ArticleRepository articleRepository, ArticleAnalysisService analysisService) {
        this.articleRepository = articleRepository;
        this.analysisService = analysisService;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateArticleRequest req) {
        String id = UUID.randomUUID().toString();

        Article article = new Article(
                id,
                req.getTitle(),
                req.getSource(),
                req.getText(),
                Instant.now()
        );

        articleRepository.save(article);

        ExtractionResult extracted = analysisService.analyzeAndPersist(article);

        return Map.of(
                "articleId", id,
                "ingestedAt", article.getIngestedAt().toString(),
                "actors", extracted.actors().size(),
                "events", extracted.events().size(),
                "interactions", extracted.interactions().size()
        );
    }
}