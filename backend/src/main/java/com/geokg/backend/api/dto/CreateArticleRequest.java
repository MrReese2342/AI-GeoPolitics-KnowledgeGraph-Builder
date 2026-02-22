package com.geokg.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateArticleRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String source;

    @NotBlank
    private String text;

    public String getTitle() { return title; }
    public String getSource() { return source; }
    public String getText() { return text; }

    public void setTitle(String title) { this.title = title; }
    public void setSource(String source) { this.source = source; }
    public void setText(String text) { this.text = text; }
}