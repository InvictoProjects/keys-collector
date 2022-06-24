package com.invictoprojects.keyscollector.controller;


import com.invictoprojects.keyscollector.service.CodeUpdateGenerator;
import com.invictoprojects.keyscollector.service.CodeUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class CodeUpdateController {

    private final CodeUpdateService codeUpdateService;
    private final Environment env;

    @Autowired
    public CodeUpdateController(CodeUpdateService codeUpdateService, Environment env) {
        this.codeUpdateService = codeUpdateService;
        this.env = env;
    }

    @GetMapping(value = "search/{service}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> searchCode(@PathVariable String service, @RequestHeader("x-github-api-token") String token) {
        String keyWord = env.getProperty(service);
        return codeUpdateService.streamCodeUpdates(service, new CodeUpdateGenerator("token "+token, keyWord));
    }

}
