package com.invictoprojects.keyscollector.service;

import com.invictoprojects.keyscollector.exception.SearchResultLimitException;
import com.invictoprojects.keyscollector.model.CodeUpdate;
import com.invictoprojects.keyscollector.model.CodeUpdates;
import com.invictoprojects.keyscollector.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class CodeUpdateServiceTest {

    private CodeUpdateGenerator codeUpdateGenerator;
    private LanguageService languageService;
    private CodeUpdateService codeUpdateService;

    @BeforeEach
    void setUp() {
        languageService = Mockito.mock(LanguageService.class);
        codeUpdateGenerator = Mockito.mock(CodeUpdateGenerator.class);
        codeUpdateService = new CodeUpdateService(languageService, new StatisticsService());
    }

    @Test
    void streamCodeUpdates() {
        Mockito.when(codeUpdateGenerator.getNextPage())
                .thenReturn(Mono.just(new CodeUpdates(List.of(
                        new CodeUpdate(
                                "file.extension",
                                "test/file.extension",
                                "https://api.github.com/repositories/1/contents/test/file.extension",
                                "FirstRepository",
                                List.of("test1=TESTKEY", "test2=TESTKEY")
                        ),
                        new CodeUpdate(
                                "file.nolang",
                                "test/file.nolang",
                                "https://api.github.com/repositories/1/contents/test/file.nolang",
                                "FirstRepository",
                                List.of("test=TESTKEY")
                        ),
                        new CodeUpdate(
                                "file.extension",
                                "test/file.extension",
                                "https://api.github.com/repositories/1/contents/test/file.extension",
                                "FirstRepository",
                                List.of("@test")
                        )
                ))))
                .thenReturn(Mono.just(new CodeUpdates(List.of(
                        new CodeUpdate(
                                "file.ext1",
                                "test/file.ext1",
                                "https://api.github.com/repositories/2/contents/test/file.ext1",
                                "SecondRepository",
                                List.of("test=TESTKEY")
                        ),
                        new CodeUpdate(
                                "file.ext2",
                                "test/file.ext2",
                                "https://api.github.com/repositories/3/contents/test/file.ext2",
                                "ThirdRepository",
                                List.of("test=TESTKEY")
                        ),
                        new CodeUpdate(
                                "file.ext2",
                                "test/file.ext2",
                                "https://api.github.com/repositories/4/contents/test/file.ext2",
                                "FourthRepository",
                                List.of("test=TESTKEY")
                        )
                ))))
                .thenReturn(Mono.error(new SearchResultLimitException("Exception")));
        Mockito.when(languageService.resolveLanguageByExtension(".extension"))
                .thenReturn("Language");
        Mockito.when(languageService.resolveLanguageByExtension(".ext1"))
                .thenReturn("Lang-1");
        Mockito.when(languageService.resolveLanguageByExtension(".ext2"))
                .thenReturn("Lang-2");
        Mockito.when(languageService.resolveLanguageByExtension(".nolang"))
                .thenReturn("Undetermined");

        Flux<Message> result = codeUpdateService.streamCodeUpdates(codeUpdateGenerator, Pattern.compile("TESTKEY"));

        StepVerifier.create(result)
                .expectSubscription()
                .expectNext(new Message("TESTKEY", "file.extension", "FirstRepository", Map.of("Language", 1), true))
                .expectNext(new Message("TESTKEY", "file.extension", "FirstRepository", Map.of("Language", 2), false))
                .expectNext(new Message("TESTKEY", "file.nolang", "FirstRepository", Map.of("Language", 2, "Undetermined", 1), false))
                .expectNext(new Message("TESTKEY", "file.ext1", "SecondRepository", Map.of("Language", 2, "Undetermined", 1, "Lang-1", 1), true))
                .expectNext(new Message("TESTKEY", "file.ext2", "ThirdRepository", Map.of("Language", 2, "Undetermined", 1, "Lang-1", 1), true))
                .expectNext(new Message("TESTKEY", "file.ext2", "FourthRepository", Map.of("Language", 2, "Lang-2", 2, "Undetermined", 1), true))
                .expectError(SearchResultLimitException.class)
                .verify();
    }

    @Test
    void parseCodeUpdates() {
        CodeUpdate codeUpdate = new CodeUpdate(
                "file1.extension",
                "test/file1.extension",
                "https://api.github.com/repositories/1/contents/test/file1.extension",
                "TestRepository",
                List.of("test=TESTKEY", "@test")
        );
        Pattern pattern = Pattern.compile("TESTKEY");

        Flux<Tuple3<String, String, String>> result = codeUpdateService.parseCodeUpdates(codeUpdate, pattern);

        StepVerifier.create(result)
                .expectSubscription()
                .expectNext(Tuples.of("TESTKEY", codeUpdate.getName(), codeUpdate.getRepositoryName()))
                .expectComplete()
                .verify();
    }

    @Test
    void getCodeUpdateFlux() {
        CodeUpdate codeUpdate1 = new CodeUpdate(
                "file1.extension",
                "test/file1.extension",
                "https://api.github.com/repositories/1/contents/test/file1.extension",
                "TestRepository",
                List.of("test=TESTKEY")
        );
        CodeUpdate codeUpdate2 = new CodeUpdate(
                "file2.extension",
                "test/file2.extension",
                "https://api.github.com/repositories/1/contents/test/file2.extension",
                "TestRepository",
                List.of("test=TESTKEY")
        );
        CodeUpdate codeUpdate3 = new CodeUpdate(
                "file1.nolang",
                "test/file1.nolang",
                "https://api.github.com/repositories/2/contents/test/file1.nolang",
                "SecondRepository",
                List.of("test=TESTKEY")
        );

        Mockito.when(codeUpdateGenerator.getNextPage())
                .thenReturn(Mono.just(new CodeUpdates(List.of(codeUpdate1, codeUpdate2))))
                .thenReturn(Mono.just(new CodeUpdates(List.of(codeUpdate3))))
                .thenReturn(Mono.error(new SearchResultLimitException("Exception")));

        Flux<CodeUpdate> result = codeUpdateService.getCodeUpdateFlux(codeUpdateGenerator);

        StepVerifier.create(result)
                .expectSubscription()
                .expectNext(codeUpdate1)
                .expectNext(codeUpdate2)
                .expectNext(codeUpdate3)
                .expectError(SearchResultLimitException.class)
                .verify();
    }

}
