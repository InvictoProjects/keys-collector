package com.invictoprojects.keyscollector.controller;


import com.invictoprojects.keyscollector.service.CodeUpdateGenerator;
import com.invictoprojects.keyscollector.service.CodeUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class CodeUpdateController {

    private final CodeUpdateService codeUpdateService;

    @Autowired
    public CodeUpdateController(CodeUpdateService codeUpdateService) {
        this.codeUpdateService = codeUpdateService;
    }

    @GetMapping(value = "search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> searchCode(@RequestHeader("x-github-api-token") String token) {
        return codeUpdateService.streamCodeUpdates(new CodeUpdateGenerator("token "+token));
    }

}
