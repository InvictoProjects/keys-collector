package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.model.CodeUpdates;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CodeUpdateService {

    public Mono<CodeUpdates> streamCodeUpdates() {
        String url = "https://api.github.com";
        WebClient client = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "token <your token>")
                .build();

        return client.get()
                .uri("search/code?q=awsaccess")
                .retrieve()
                .bodyToMono(CodeUpdates.class);
    }

}
