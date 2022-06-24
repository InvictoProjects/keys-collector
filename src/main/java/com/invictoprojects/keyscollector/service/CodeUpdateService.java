package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.model.CodeUpdates;
import com.invictoprojects.keyscollector.model.TextMatches;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeUpdateService {

    private int page = 0;
    private final Map<String, Integer> extension2Frequency = new HashMap<>();
    private final Set<String> projects = new LinkedHashSet<>();

    public Flux<String> streamCodeUpdates(CodeUpdateGenerator generator) {
        return Flux.generate((SynchronousSink<Mono<CodeUpdates>> synchronousSink) -> {
                    Mono<CodeUpdates> codeUpdate = generator.next();
                    if (codeUpdate != null) {
                        synchronousSink.next(codeUpdate);
                    } else {
                        synchronousSink.complete();
                    }
                })
                .flatMap(Flux::from)
                .filter(codeUpdates -> codeUpdates.getItems() != null)
                .flatMap(codeUpdates -> {
                    System.out.println(++page);
                    return Flux.fromStream(codeUpdates.getItems().stream());
                })
                .flatMap(codeUpdate -> Flux.fromStream(codeUpdate.getTextMatches().stream()
                        .map(textMatches -> Tuples.of(codeUpdate.getName(), textMatches, codeUpdate.getRepository().getName()))))
                .map(tuple3 -> {
                    String fileName = tuple3.getT1();
                    TextMatches textMatches = tuple3.getT2();
                    String projectName = tuple3.getT3();

                    String s = textMatches.getFragment();
                    Pattern pattern = Pattern.compile("(A3T[A-Z0-9]|AKIA|AGPA|AIDA|AROA|AIPA|ANPA|ANVA|ASIA)[A-Z0-9]{16}");
                    Matcher matcher = pattern.matcher(s);

                    if (matcher.find()) {
                        String[] arr = fileName.split("\\.");
                        String extension = arr[arr.length-1];

                        if (!extension2Frequency.containsKey(extension)) {
                            extension2Frequency.put(extension, 0);
                        }

                        Integer currAmount = extension2Frequency.get(extension);
                        extension2Frequency.put(extension, ++currAmount);

                        String result = matcher.group();
                        StringBuilder extensionResults = new StringBuilder();
                        extension2Frequency.forEach((key, value) -> extensionResults.append(key)
                                .append(" ")
                                .append(value)
                                .append("\n"));

                        boolean isNewProject = !projects.contains(projectName);
                        projects.add(projectName);

                        return Tuples.of(result, extensionResults.toString(), isNewProject?"New project with keys exposure!!!":Strings.EMPTY);
                    }
                    return Tuples.of(Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);
                })
                .filter(t -> !t.getT1().isEmpty() && !t.getT2().isEmpty() && !t.getT3().isEmpty())
                .map(tuple -> tuple.getT1() + "\n" + tuple.getT2() + "\n" + tuple.getT3())
                .delayElements(Duration.ofSeconds(1));
    }

}
