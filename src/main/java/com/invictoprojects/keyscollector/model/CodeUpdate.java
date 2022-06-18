package com.invictoprojects.keyscollector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CodeUpdate {

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("url")
    private String url;

    @JsonProperty("repository")
    private Repository repository;

    @JsonProperty("text_matches")
    private List<TextMatches> textMatches;

}
