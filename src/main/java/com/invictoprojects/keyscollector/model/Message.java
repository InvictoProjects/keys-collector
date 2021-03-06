package com.invictoprojects.keyscollector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@AllArgsConstructor
@Data
public class Message {
    @JsonProperty("key")
    private String key;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("language_stats")
    private Map<String, Integer> languageStats;

    @JsonProperty("is_new_project")
    private Boolean isNewProject;
}
