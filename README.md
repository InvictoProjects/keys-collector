# Keys Collector
A tool that scan GitHub Code updates using Search API and find exposed API keys(AWS, Facebook, Twitter, LinkedIn) which might be collected and used later.
## Implementation
`CodeUpdateController` is Spring WebFlux controller that contains HTTP GET mapping with `service` path variable to select API keys to search. You also need to include a GitHub token in `x-github-api-token` header. Controller method retrieves keywords and RegEx patterns from properties file using `PropertiesService`, creates CodeUpdateGenerator and returns result of `streamCodeUpdates` method of CodeUpdateService. Response is streamed using Newline delimited JSON.

`CodeUpdateGenerator` constructs search query and makes request to GitHub Search API using Spring Web Client. Response status code `403 Forbidden` means that API limit is exceeded so generator waits for one minute and retries request. Successful responses are transformed to `Mono<CodeUpdates>`.

`CodeUpdateService` contains methods that create Flux from CodeUpdateGenerator and transform it to Flux with Response messages:
- `getCodeUpdateFlux` method generates Flux using CodeUpdateGenerator and flatmaps `Flux<CodeUpdates>` to `Flux<CodeUpdate>` by streaming CodeUpdates items list.
- `parseCodeUpdates` method flattens CodeUpdate textMatches list, filters CodeUpdates that don't match RegEx pattern and returns Tuple with found key, filename and repository name.
- `collectLanguageStats` method gets file extension from filename, resolves language from extension by call to `LanguageService` and adds that language to Map using `StatisticsService`.
- `isNewProject` method returns whether repository name was already saved to `StatisticsService` Set and saves it otherwise.

Finally `Tuple` is mapped to `Message` class that contains:
- `key` - exposed key
- `language_stats` - top 3 languages in which keys was exposed most frequently
- `project_name` - repository name 
- `is_new_project` - boolean value that highlights new projects which have key exposure.
## Sequence diagram
![sequence-diagram](docs/Diagram.png)
