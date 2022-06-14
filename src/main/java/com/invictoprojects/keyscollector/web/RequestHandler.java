package com.invictoprojects.keyscollector.web;

import com.invictoprojects.keyscollector.model.CodeUpdate;
import com.invictoprojects.keyscollector.model.CodeUpdates;
import com.invictoprojects.keyscollector.service.CodeUpdateGenerator;
import com.invictoprojects.keyscollector.service.CodeUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RequestHandler {
    private final CodeUpdateService codeUpdateService;

    @Autowired
    public RequestHandler(CodeUpdateService codeUpdateService) {
        this.codeUpdateService = codeUpdateService;
    }

    public Mono<ServerResponse> streamCodeUpdates(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(codeUpdateService.streamCodeUpdates(new CodeUpdateGenerator()), CodeUpdate.class);
    }
}
