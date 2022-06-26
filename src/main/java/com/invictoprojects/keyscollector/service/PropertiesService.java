package com.invictoprojects.keyscollector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Service
public class PropertiesService {

    private final Environment env;

    @Autowired
    public PropertiesService(Environment env) {
        this.env = env;
    }

    public String getKeyWords(String service) {
        String[] serviceKeys = requireNonNull(env.getProperty("list." + service + ".keys")).split(",");
        return Arrays.stream(serviceKeys)
                .map(x -> service + x)
                .reduce((x1, x2) -> x1 + " " + x2)
                .get();
    }

    public Pattern getPattern(String service) {
        String[] serviceKeys = requireNonNull(env.getProperty("list." + service + ".keys")).split(",");
        String keys = Arrays.stream(serviceKeys)
                .map(x -> env.getProperty("regexp." + service + "." + x))
                .map(x -> serviceKeys.length > 1 ? "(" + x + ")" : x)
                .reduce((x1, x2) -> x1 + "|" + x2)
                .map(x -> serviceKeys.length > 1 ? "(" + x + ")" : x)
                .get();
        return Pattern.compile(keys);
    }
}
