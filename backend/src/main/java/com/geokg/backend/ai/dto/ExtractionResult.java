package com.geokg.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractionResult(
        List<ActorDto> actors,
        List<EventDto> events,
        List<InteractionDto> interactions
) {
    public static ExtractionResult empty() {
        return new ExtractionResult(List.of(), List.of(), List.of());
    }
}