package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.exception.RequestLimitException;
import com.invictoprojects.keyscollector.model.CodeUpdates;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

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
                    httpHeaders.set("User-Agent", "keys-collector");
                })
                .build();
    }

    public Mono<CodeUpdates> getNextPage() {
        currentPage++;
        return client.get()
                .uri(searchApiUri + "&page=" + currentPage)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.equals(HttpStatus.FORBIDDEN),
                        response -> Mono.error(new RequestLimitException("Rate limit is exceeded", response.rawStatusCode())))
                .onStatus(httpStatus -> httpStatus.equals(HttpStatus.UNPROCESSABLE_ENTITY), response -> null)
                .bodyToMono(CodeUpdates.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofMinutes(1))
                        .filter(RequestLimitException.class::isInstance));
    }
}
