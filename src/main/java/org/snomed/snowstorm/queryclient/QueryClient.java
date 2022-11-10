package org.snomed.snowstorm.queryclient;

import org.snomed.snowstorm.core.data.domain.Concept;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.data.domain.Relationship;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

public class QueryClient {

    private final WebClient webClient;

    private final String BRANCH = "MAIN";

    public QueryClient(){
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8080/")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-X-900000000000509007", "en-X-900000000000508004", "en")
                .build();
    }

    public Optional<Concept> findConcept(String conceptId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(BRANCH + "/concepts/" + conceptId).build())
                .retrieve()
                .bodyToMono(Concept.class)
                .onErrorStop()
                .blockOptional();
    }


    public List<Concept> findConceptParents(String conceptId) {
        Flux<Concept> conceptFlux = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("browser/" + BRANCH + "/concepts/" + conceptId + "/parents")
                        .queryParam("form", "inferred")
                        .queryParam("includeDescendantCount", "false")
                        .build())
                .retrieve()
                .bodyToFlux(Concept.class);
        return conceptFlux.toStream().toList();
    }

    public List<Concept> findConceptChildren(String snomedId) {
        Flux<Concept> conceptFlux = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("browser/" + BRANCH + "/concepts/" + snomedId + "/children")
                        .queryParam("form", "inferred")
                        .queryParam("includeDescendantCount", "false")
                        .build())
                .retrieve()
                .bodyToFlux(Concept.class);
        return conceptFlux.toStream().toList();
    }

    public Optional<Relationship> findRelationship(String relationshipId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BRANCH + "/relationships/" + relationshipId)
                        .build())
                .retrieve()
                .bodyToMono(Relationship.class)
                .onErrorStop()
                .blockOptional();
    }

    public Optional<Description> findDescription(String descriptionId){
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BRANCH + "/descriptions/" + descriptionId)
                        .build())
                .retrieve()
                .bodyToMono(Description.class)
                .onErrorStop()
                .blockOptional();
    }




    public static void main(String[] args) {
        final String id = "3723501019";
        QueryClient queryClient = new QueryClient();

        Description description = queryClient.findDescription(id).orElseThrow();
        System.out.println(description);
    }
}
