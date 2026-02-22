package com.geokg.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActorDto(
        String name,
        String type // COUNTRY | ORG | PERSON
) {}