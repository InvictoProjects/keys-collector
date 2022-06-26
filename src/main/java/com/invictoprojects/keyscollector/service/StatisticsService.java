package com.invictoprojects.keyscollector.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StatisticsService {

    private final Map<String, Integer> programmingLanguageStats = new ConcurrentHashMap<>();

    private final Set<String> projects = Collections.synchronizedSet(new LinkedHashSet<>());

    public void saveProject(String projectName) {
        projects.add(projectName);
    }

    public boolean isProjectAlreadySaved(String projectName) {
        return projects.contains(projectName);
    }

    public Map<String, Integer> getProgrammingLanguageStats() {
        return programmingLanguageStats;
    }

    public void saveProgrammingLanguage(String language) {
        programmingLanguageStats.putIfAbsent(language, 0);
        Integer currAmount = programmingLanguageStats.get(language);
        programmingLanguageStats.put(language, ++currAmount);
    }

}
