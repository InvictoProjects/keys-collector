package com.invictoprojects.keyscollector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TextMatches {

    @JsonProperty("fragment")
    private String fragment;

}
