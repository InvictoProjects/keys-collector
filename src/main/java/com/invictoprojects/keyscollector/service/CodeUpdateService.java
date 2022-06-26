package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.model.CodeUpdate;
import com.invictoprojects.keyscollector.model.CodeUpdates;
import com.invictoprojects.keyscollector.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CodeUpdateService {

    private final Map<String, Integer> programmingLanguageStats = new ConcurrentHashMap<>();
    private final Set<String> projects = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Environment env;

    private final LanguageService languageService;

    @Autowired
    public CodeUpdateService(Environment env, LanguageService languageService) {
        this.env = env;
        this.languageService = languageService;
    }

    public Flux<Message> streamCodeUpdates(String key, CodeUpdateGenerator generator) {
        String regex = env.getProperty("regexp.AccessKey." + key.toLowerCase());
        if (regex == null) {
            throw new IllegalArgumentException("Please provide key that exists in config file!");
        }
        return getCodeUpdates(generator)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(codeUpdate -> parseCodeUpdates(codeUpdate, Pattern.compile(regex)))
                .doOnNext(tuple -> collectLanguageStats(tuple.getT2()))
                .map(tuple -> new Message(
                        tuple.getT1(),
                        getTopExtensionStats(),
                        tuple.getT3(),
                        isNewProject(tuple.getT3())
                ));
    }

    private Flux<CodeUpdate> getCodeUpdates(CodeUpdateGenerator generator) {
        return Flux.generate((SynchronousSink<Mono<CodeUpdates>> synchronousSink) -> {
                    Mono<CodeUpdates> codeUpdate = generator.next();
                    if (codeUpdate != null) {
                        synchronousSink.next(codeUpdate);
                    } else {
                        synchronousSink.complete();
                    }
                })
                .flatMap(this::getCodeUpdateFlux, 1, 1);
    }

    private Flux<Tuple3<String, String, String>> parseCodeUpdates(CodeUpdate codeUpdate, Pattern pattern) {
        return Flux.fromStream(codeUpdate.getTextMatches().stream())
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(Matcher::group)
                .map(key -> Tuples.of(key, codeUpdate.getName(), codeUpdate.getRepositoryName()));
    }

    private void collectLanguageStats(String filename) {
        String[] arr = filename.split("\\.");
        String extension = arr.length == 1 ? "Undetermined" : "." + arr[arr.length - 1];
        String language = languageService.resolveLanguageByExtension(extension);
        programmingLanguageStats.putIfAbsent(language, 0);
        Integer currAmount = programmingLanguageStats.get(language);
        programmingLanguageStats.put(language, ++currAmount);
    }

    private Boolean isNewProject(String projectName) {
        Boolean result = !projects.contains(projectName);
        projects.add(projectName);
        return result;
    }

    private Flux<CodeUpdate> getCodeUpdateFlux(Mono<CodeUpdates> codeUpdatesMono) {
        return Flux.from(codeUpdatesMono)
                .filter(codeUpdates -> codeUpdates.getItems() != null)
                .flatMap(codeUpdates -> Flux.fromStream(codeUpdates.getItems().stream()));
    }

    private Map<String, Integer> getTopExtensionStats() {
        return programmingLanguageStats.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }
}
