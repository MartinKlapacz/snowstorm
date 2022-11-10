package org.snomed.snowstorm.queryclient;

import lombok.NoArgsConstructor;
import org.snomed.snowstorm.core.data.domain.Concept;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.List;

public class QueryClient {

    private final WebClient webClient;

    public QueryClient(){
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8080/")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-X-900000000000509007", "en-X-900000000000508004", "en")
                .build();
    }

    public Concept getConcept(String snomedId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("MAIN/concepts/" + snomedId).build())
                .retrieve()
                .bodyToMono(Concept.class)
                .onErrorStop()
                .block();
    }


    public List<Concept> getConceptParents(String snomedId) {
        Flux<Concept> conceptFlux = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("browser/MAIN/concepts/" + snomedId + "/parents")
                        .queryParam("form", "inferred")
                        .queryParam("includeDescendantCount", "false")
                        .build())
                .retrieve()
                .bodyToFlux(Concept.class);
        return conceptFlux.toStream().toList();
    }

    public List<Concept> getConceptChildren(String snomedId) {
        Flux<Concept> conceptFlux = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("browser/MAIN/concepts/" + snomedId + "/children")
                        .queryParam("form", "inferred")
                        .queryParam("includeDescendantCount", "false")
                        .build())
                .retrieve()
                .bodyToFlux(Concept.class);
        return conceptFlux.toStream().toList();
    }



    public static void main(String[] args) {
        final String snomedId = "386661006";
        QueryClient queryClient = new QueryClient();

        System.out.println(queryClient.getConcept(snomedId));

        List<Concept> concepts = queryClient.getConceptChildren(snomedId);
        System.out.println(concepts);
    }
}
