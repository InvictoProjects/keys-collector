package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.exception.KeysCollectorException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LanguageService {

    private final Logger logger = LoggerFactory.getLogger(LanguageService.class);
    private final Map<String, String> extension2Language = new LinkedHashMap<>();

    @PostConstruct
    private void createExtensionToLanguageMap() throws KeysCollectorException {
        try {
            logger.info("Trying to cache languages.json");
            String file = "src/main/resources/languages.json";
            byte[] bytes = Files.readAllBytes(Paths.get(file));
            String json = new String(bytes);
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                if (jsonObject.has("extensions")) {
                    JSONArray extensions = jsonObject.getJSONArray("extensions");
                    for (int j = 0; j < extensions.length(); j++) {
                        String extension = (String) extensions.get(j);
                        extension2Language.put(extension, name);
                    }
                }
            }
            logger.info("Successfully cached languages.json");
        } catch (IOException e) {
            throw new KeysCollectorException("Failed to cache languages.json");
        }
    }

    public String resolveLanguageByExtension(String extension) {
        if (extension2Language.containsKey(extension)) {
            return extension2Language.get(extension);
        }
        logger.warn("Unknown extension: {}", extension);
        return "Undetermined";
    }

}

