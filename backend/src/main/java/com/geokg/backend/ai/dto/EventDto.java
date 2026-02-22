package com.geokg.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventDto(
        String type,        // THREAT | CLASH | STRIKE | SANCTION | ...
        String occurredAt,  // YYYY-MM-DD (si inconnu: null)
        String summary,
        Double confidence
) {}