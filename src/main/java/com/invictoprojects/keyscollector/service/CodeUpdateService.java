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
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CodeUpdateService {

    private final Map<String, Integer> extensionStats = new HashMap<>();
    private final Set<String> projects = new LinkedHashSet<>();
    private final Environment env;

    @Autowired
    public CodeUpdateService(Environment env) {
        this.env = env;
    }

    public Flux<Message> streamCodeUpdates(String service, String token) {
        String key = env.getProperty(service);
        if (key == null) {
            throw new IllegalArgumentException("Please provide key that exists in config file!");
        }
        CodeUpdateGenerator generator = new CodeUpdateGenerator("token "+ token , key);

        String regex = env.getProperty("regexp." + service.toLowerCase() + ".accesskey");
        if (regex == null) {
            throw new IllegalArgumentException("Please provide key that exists in config file!");
        }
        Pattern pattern = Pattern.compile(regex);

        return getCodeUpdates(generator)
                .flatMap(codeUpdate -> parseCodeUpdates(codeUpdate, pattern))
                .doOnNext(tuple -> collectExtensionStats(tuple.getT2()))
                .map(tuple -> new Message(tuple.getT1(), getTopExtensionStats(), isNewProject(tuple.getT3())))
                .delayElements(Duration.ofSeconds(1));
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
                .flatMap(this::getCodeUpdateFlux);
    }

    private Flux<Tuple3<String, String, String>> parseCodeUpdates(CodeUpdate codeUpdate, Pattern pattern) {
        return Flux.fromStream(codeUpdate.getTextMatches().stream())
                .map(textMatches -> pattern.matcher(textMatches.getFragment()))
                .filter(Matcher::find)
                .map(Matcher::group)
                .map(key -> Tuples.of(key, codeUpdate.getName(), codeUpdate.getRepository().getName()));
    }

    private void collectExtensionStats(String filename) {
        String[] arr = filename.split("\\.");
        String extension = arr[arr.length - 1];
        if (!extensionStats.containsKey(extension)) {
            extensionStats.put(extension, 0);
        }
        Integer currAmount = extensionStats.get(extension);
        extensionStats.put(extension, ++currAmount);
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
        return extensionStats.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }
}
