package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.model.CodeUpdates;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class CodeUpdateGenerator {

    private static final String BASE_URL = "https://api.github.com";
    private static final String ACCEPT_HEADER = "application/vnd.github.v3.text-match+json";

    private String searchApiUri = "search/code?sort=indexed&order=desc&per_page=30&q=";
    private final String authorizationToken;
    private final WebClient client;

    private long currentPage;

    public CodeUpdateGenerator(String token, String keyWord) {
        this.authorizationToken = token;
        searchApiUri += keyWord;
        client = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("Authorization", authorizationToken);
                    httpHeaders.set("Accept", ACCEPT_HEADER);
                })
                .build();
    }

    public CodeUpdates getNextPage() {
        return getNextPageMono()
                .block();
    }

    public Mono<CodeUpdates> getNextPageMono() {
        currentPage++;
        return client.get()
                .uri(searchApiUri+ "&page=" + currentPage)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.FORBIDDEN)) {
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        currentPage--;
                        return getNextPageMono();
                    } else if (clientResponse.statusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                        return Mono.empty();
                    } else {
                        return clientResponse.bodyToMono(CodeUpdates.class);
                    }
                });
    }
}
