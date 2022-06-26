package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.model.CodeUpdate;
import com.invictoprojects.keyscollector.model.CodeUpdates;
import com.invictoprojects.keyscollector.model.Message;
import org.apache.logging.log4j.util.Strings;
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

import static java.util.Objects.requireNonNull;

@Service
public class CodeUpdateService {

    private final Map<String, Integer> programmingLanguageStats = new ConcurrentHashMap<>();
    private final Set<String> projects = Collections.synchronizedSet(new LinkedHashSet<>());
    private String keys = Strings.EMPTY;
    private String keyWords = Strings.EMPTY;
    private final Environment env;

    private final LanguageService languageService;

    @Autowired
    public CodeUpdateService(Environment env, LanguageService languageService) {
        this.env = env;
        this.languageService = languageService;
    }

    public Flux<Message> streamCodeUpdates(String service, String token) {
        String[] serviceKeys = requireNonNull(env.getProperty("list." + service + ".keys")).split(",");

        Flux.fromArray(serviceKeys)
                .mapNotNull(x -> env.getProperty("regexp." + service + "." + x))
                .map(x -> serviceKeys.length > 1 ? "(" + x + ")" : x)
                .reduce((x1, x2) -> x1 + "|" + x2)
                .map(x -> serviceKeys.length > 1 ? "(" + x + ")" : x)
                .subscribe(result -> keys = result);
        Pattern pattern = Pattern.compile(keys);

        Flux.fromArray(serviceKeys)
                .mapNotNull(x -> service + x)
                .reduce((x1, x2) -> x1 + " " + x2)
                .subscribe(result -> keyWords = result);
        CodeUpdateGenerator generator = new CodeUpdateGenerator("token "+ token , keyWords);

        return getCodeUpdates(generator)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(codeUpdate -> parseCodeUpdates(codeUpdate, pattern))
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
