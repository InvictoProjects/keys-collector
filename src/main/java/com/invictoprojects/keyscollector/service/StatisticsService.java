package com.invictoprojects.keyscollector.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    public void saveProgrammingLanguage(String language) {
        programmingLanguageStats.putIfAbsent(language, 0);
        Integer currAmount = programmingLanguageStats.get(language);
        programmingLanguageStats.put(language, ++currAmount);
    }

    public Map<String, Integer> getTopLanguageStats() {
        return programmingLanguageStats.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

}
