package com.geokg.backend.api;

import com.geokg.backend.api.dto.CreateArticleRequest;
import com.geokg.backend.domain.Article;
import com.geokg.backend.repo.ArticleRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleRepository articleRepository;

    public ArticleController(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
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

        return Map.of(
                "articleId", id,
                "ingestedAt", article.getIngestedAt().toString()
        );
    }

    @GetMapping("/{id}")
    public Article getById(@PathVariable String id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + id));
    }
}