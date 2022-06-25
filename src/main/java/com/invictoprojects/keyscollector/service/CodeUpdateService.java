package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.model.CodeUpdate;
import com.invictoprojects.keyscollector.model.CodeUpdates;
import com.invictoprojects.keyscollector.model.TextMatches;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class CodeUpdateService {

    private Pattern pattern;
    private int page = 0;
    private final Map<String, Integer> extension2Frequency = new HashMap<>();
    private final Set<String> projects = new LinkedHashSet<>();
    private final Environment env;

    @Autowired
    public CodeUpdateService(Environment env) {
        this.env = env;
    }

    public Flux<String> streamCodeUpdates(String key, CodeUpdateGenerator generator) {
        String regex = env.getProperty("regexp.AccessKey." + key.toLowerCase());
        if (regex == null) {
            return Flux.just(Strings.EMPTY);
        }
        pattern  = Pattern.compile(regex);
        return Flux.generate((SynchronousSink<Mono<CodeUpdates>> synchronousSink) -> {
                    Mono<CodeUpdates> codeUpdate = generator.next();
                    if (codeUpdate != null) {
                        synchronousSink.next(codeUpdate);
                    } else {
                        synchronousSink.complete();
                    }
                })
                .log()
                .flatMap(this::getCodeUpdateFlux, 1, 1)
                .flatMap(this::getSearchInfoFlux)
                .map(this::processSearchInfo)
                .filter(StringUtils::hasLength)
                .delayElements(Duration.ofSeconds(15));
    }

    public Flux<CodeUpdate> getCodeUpdateFlux(Mono<CodeUpdates> codeUpdatesMono) {
        return Flux.from(codeUpdatesMono)
                .filter(codeUpdates -> codeUpdates.getItems() != null)
                .flatMap(codeUpdates -> {
                    System.out.println(++page);
                    return Flux.fromStream(codeUpdates.getItems().stream());
                });
    }

    public Flux<Tuple3<String, TextMatches, String>> getSearchInfoFlux(CodeUpdate codeUpdate) {
        return Flux.fromStream(codeUpdate.getTextMatches().stream()
                .map(textMatches -> Tuples.of(codeUpdate.getName(), textMatches, codeUpdate.getRepository().getName())));
    }

    public String processSearchInfo(Tuple3<String, TextMatches, String> searchInfo) {
        String fileName = searchInfo.getT1();
        TextMatches textMatches = searchInfo.getT2();
        String projectName = searchInfo.getT3();

        String s = textMatches.getFragment();
        Matcher matcher = pattern.matcher(s);

        if (matcher.find()) {
            String extension = getExtension(fileName);

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

            return createResponse(result, String.valueOf(extensionResults), isNewProject?"New project with keys exposure!!!":Strings.EMPTY);
        }
        return Strings.EMPTY;
    }

    public String getExtension(String fileName) {
        String[] arr = fileName.split("\\.");
        return arr[arr.length-1];
    }

    public String createResponse(String result, String extension, String projectName) {
        StringBuilder response = new StringBuilder();
        Stream.of(result, extension, projectName)
                .filter(Objects::nonNull)
                .forEach(s -> response.append(s)
                        .append("\n"));
        return response.toString();
    }

}
