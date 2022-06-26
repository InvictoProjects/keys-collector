package com.invictoprojects.keyscollector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CodeUpdate {

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("url")
    private String url;

    private String repositoryName;

    @JsonProperty("repository")
    private void unpackRepositoryName(Map<String, Object> repository) {
        this.repositoryName = (String)repository.get("name");
    }

    private List<String> textMatches;

    @JsonProperty("text_matches")
    private void unpackFragments(List<Map<String, Object>> textMatches) {
        this.textMatches = textMatches.stream()
                .map(textMatch -> (String)textMatch.get("fragment"))
                .collect(Collectors.toList());
    }

}
