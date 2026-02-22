package com.geokg.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InteractionDto(
        String from,
        String to,
        String type,
        Integer polarity,     // -1 hostile, +1 coop
        Integer weight,       // 1..5 intensité
        String occurredAt,    // YYYY-MM-DD (si inconnu: null)
        String evidence,      // extrait court
        Double confidence
) {}