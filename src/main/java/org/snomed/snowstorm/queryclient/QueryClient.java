package org.snomed.snowstorm.queryclient;

import org.snomed.snowstorm.core.data.domain.Concept;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.data.domain.Relationship;
import org.snomed.snowstorm.rest.pojo.ConceptDescriptionsResult;
import org.snomed.snowstorm.rest.pojo.ItemsPage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class QueryClient {

    private final WebClient webClient;

    private final int OFFSET_DEFAULT = 0;
    private final int LIMIT_DEFAULT = 50;

    private final String BRANCH = "MAIN";

    public QueryClient() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8080/")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-X-900000000000509007", "en-X-900000000000508004", "en")
                .build();

    }

    public static void main(String[] args) {
        final String id = "386661006";
        QueryClient queryClient = new QueryClient();

        List<Concept> concepts = new ArrayList<>();
        queryClient.findConceptChildrenUntilLevel(id, 1, concepts);
        concepts.forEach(System.out::println);
        System.out.println(concepts.size());
    }

    public Optional<Concept> findConcept(String conceptId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(BRANCH + "/concepts/" + conceptId).build())
                .retrieve()
                .bodyToMono(Concept.class)
                .onErrorStop()
                .blockOptional();
    }

    public List<Concept> findConcepts(int offset, int limit) {
        ItemsPage<Concept> conceptItemsPage = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Paths.get(BRANCH, "concepts").toString())
                        .queryParam("number", offset)
                        .queryParam("size", limit)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ItemsPage<Concept>>() {
                })
                .onErrorStop()
                .block();

        Objects.requireNonNull(conceptItemsPage);
        return conceptItemsPage.getItems().stream().collect(Collectors.toList());
    }

    public List<Concept> findConceptParents(String conceptId) {
        Flux<Concept> conceptFlux = webClient.get()
                .uri(uriBuilder -> {
                    URI uri = uriBuilder
                            .path(Paths.get("browser", BRANCH, "concepts", conceptId, "parents").toString())
                            .queryParam("form", "inferred")
                            .queryParam("includeDescendantCount", "false")
                            .build();
                    System.out.println(uri);
                    return uri;
                })
                .retrieve()
                .bodyToFlux(Concept.class);
        return conceptFlux.toStream().collect(Collectors.toList());
    }

    public List<Concept> findConceptChildren(String snomedId) {
        List<Concept> conceptList = Collections.synchronizedList(new ArrayList<>());
        findConceptChildrenUntilLevel(snomedId, 1, conceptList);
        return conceptList;
    }

    public void findConceptChildrenUntilLevel(String snomedId, int level, List<Concept> results) {
        Flux<Concept> conceptFlux = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Paths.get("browser", BRANCH, "concepts", snomedId, "children").toString())
                        .queryParam("form", "inferred")
                        .queryParam("includeDescendantCount", "false")
                        .build())
                .retrieve()
                .bodyToFlux(Concept.class);
        results.addAll(conceptFlux.toStream().toList());
        if (level > 1) {
            for (Concept concept : conceptFlux.toIterable()) {
                findConceptChildrenUntilLevel(concept.getId(), level - 1, results);
            }
        }
    }

    public Optional<Relationship> findRelationship(String relationshipId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Paths.get(BRANCH, "relationships", relationshipId).toString())
                        .build())
                .retrieve()
                .bodyToMono(Relationship.class)
                .onErrorStop()
                .blockOptional();
    }

    public List<Relationship> findRelationships(RelationshipQueryOptions queryOptions) {
        ItemsPage<Relationship> relationshipItemsPage = webClient
                .get()
                .uri(uriBuilder -> {
                    uriBuilder = uriBuilder.path(Paths.get(BRANCH, "relationships").toString());
                    if (queryOptions.getSourceConcept() != null) {
                        uriBuilder = uriBuilder.queryParam("source", queryOptions.getSourceConcept());
                    }
                    if (queryOptions.getDestinationConcept() != null) {
                        uriBuilder = uriBuilder.queryParam("destination", queryOptions.getDestinationConcept());
                    }
                    return uriBuilder
                            .queryParam("offset", queryOptions.getOffset() == null ? OFFSET_DEFAULT : queryOptions.getOffset())
                            .queryParam("limit", queryOptions.getLimit() == null ? LIMIT_DEFAULT : queryOptions.getLimit())
                            .build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ItemsPage<Relationship>>() {
                })
                .onErrorStop()
                .block();

        Objects.requireNonNull(relationshipItemsPage);
        return relationshipItemsPage.getItems().stream().collect(Collectors.toList());
    }

    public Optional<Description> findDescription(String descriptionId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(Paths.get(BRANCH, "descriptions", descriptionId).toString())
                        .build())
                .retrieve()
                .bodyToMono(Description.class)
                .onErrorStop()
                .blockOptional();
    }

    public Set<Description> findDescriptionsForConcept(String conceptId) {
        ConceptDescriptionsResult conceptDescriptionsResult = webClient.get()
                .uri(uriBuilder -> {
                    URI uri = uriBuilder
                            .path(Paths.get(BRANCH, "concepts", conceptId, "descriptions").toString())
                            .build();
                    System.out.println(uri);
                    return uri;
                })
                .retrieve()
                .bodyToMono(ConceptDescriptionsResult.class)
                .onErrorStop()
                .block();
        Objects.requireNonNull(conceptDescriptionsResult);
        return conceptDescriptionsResult.getConceptDescriptions();
    }
}

