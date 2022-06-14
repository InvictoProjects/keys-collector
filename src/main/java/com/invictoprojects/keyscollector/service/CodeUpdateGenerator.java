package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.model.CodeUpdates;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class CodeUpdateGenerator {

    private static final String baseUrl = "https://api.github.com";
    private static final String searchApiUri = "search/code?q=awsaccess";
    private static final String authorizationToken = "token <your token>";

    private final WebClient client = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", authorizationToken)
            .build();

    private long currentPage = 0L;

    public Mono<CodeUpdates> next() {
        currentPage++;
        return client.get()
                .uri(searchApiUri+ "&page=" + currentPage)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.FORBIDDEN)) {
                        return Mono.empty();
                    } else if (clientResponse.statusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                        return Mono.empty();
                    } else {
                        return clientResponse.bodyToMono(CodeUpdates.class);
                    }
                })
                .delaySubscription(Duration.ofSeconds(2));
    }
}
