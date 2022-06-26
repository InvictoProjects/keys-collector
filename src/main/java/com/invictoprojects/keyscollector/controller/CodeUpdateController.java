package com.invictoprojects.keyscollector.controller;


import com.invictoprojects.keyscollector.model.Message;
import com.invictoprojects.keyscollector.service.CodeUpdateGenerator;
import com.invictoprojects.keyscollector.service.CodeUpdateService;
import com.invictoprojects.keyscollector.service.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.regex.Pattern;

@RestController
public class CodeUpdateController {

    private final CodeUpdateService codeUpdateService;
    private final PropertiesService propertiesService;

    @Autowired
    public CodeUpdateController(CodeUpdateService codeUpdateService, PropertiesService propertiesService) {
        this.codeUpdateService = codeUpdateService;
        this.propertiesService = propertiesService;
    }

    @GetMapping(value = "search/{service}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Message> searchCode(@PathVariable String service, @RequestHeader("x-github-api-token") String token) {
        String keyWords = propertiesService.getKeyWords(service);
        Pattern pattern = propertiesService.getPattern(service);
        return codeUpdateService.streamCodeUpdates(new CodeUpdateGenerator("token " + token , keyWords), pattern);
    }

}
