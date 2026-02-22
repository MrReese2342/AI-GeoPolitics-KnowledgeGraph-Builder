package com.geokg.backend.repo;

import com.geokg.backend.domain.Article;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ArticleRepository extends Neo4jRepository<Article, String> {
}