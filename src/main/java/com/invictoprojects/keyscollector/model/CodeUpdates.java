package com.invictoprojects.keyscollector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CodeUpdates {

    @JsonProperty("items")
    private List<CodeUpdate> items;

}
