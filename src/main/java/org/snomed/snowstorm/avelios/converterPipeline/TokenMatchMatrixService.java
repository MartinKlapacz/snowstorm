package org.snomed.snowstorm.avelios.converterPipeline;

import lombok.Getter;
import org.elasticsearch.index.query.QueryBuilders;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.snomed.snowstorm.core.data.domain.Description.Fields.*;
import static org.snomed.snowstorm.core.data.domain.Description.Fields.TERM;

@Service
public class TokenMatchMatrixService {
    @Autowired
    ConceptService conceptService;

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Map<List<String>, Set<DescriptionMatch>> cells = new ConcurrentHashMap<>();

    private static final String DESCRIPTION_INDEX = "description";

    final int MAX_TOKEN_SUBSEQUENCE_LENGTH = 5;


    @Getter
    private List<DescriptionMatch> topScoredDescriptions = new ArrayList<>();


    private void generateLexicon(List<String> tokens) {
        int numOfCells = tokens.size() + tokens.size() * (tokens.size() - 1) / 2;
        List<Future<?>> futureList = new ArrayList<>(numOfCells);

        // for each token get the descriptionIds/wordCounts of the descriptions that contain the token
        for (String token : tokens) {
            Future<?> future = executorService.submit(() -> {
                Set<DescriptionMatch> descriptionMatchSet = getDescriptionsContainingSingleToken(token);
                cells.put(List.of(token), descriptionMatchSet);
            });
            futureList.add(future);
        }
        joinAndClearFutures(futureList);

        for (int i = 0; i < tokens.size(); i++) {
            for (int j = i + 1; j < tokens.size() && j - i + 1 <= MAX_TOKEN_SUBSEQUENCE_LENGTH; j++) {
                int finalI = i;
                int finalJ = j;
                Future<?> future = executorService.submit(() -> {
                    // get current token subsequence from indices
                    List<String> tokenSubsequence = Arrays.stream(IntStream.rangeClosed(finalI, finalJ).toArray())
                            .boxed()
                            .map(tokens::get)
                            .collect(Collectors.toList());

                    // get description Id set for each token in token subsequence
                    List<Set<DescriptionMatch>> descriptionMatchSetsToIntersect = tokenSubsequence.stream()
                            .map(List::of)
                            .map(cells::get)
                            .collect(Collectors.toList());

                    // compute their intersection
                    Set<DescriptionMatch> intersectedDescriptionMatches = new HashSet<>(descriptionMatchSetsToIntersect.get(0));
                    descriptionMatchSetsToIntersect.stream().skip(1).forEach(intersectedDescriptionMatches::retainAll);


                    // use the description size to compute the id scores
                    int numOfDistinctMatchingTokens = new HashSet<>(tokens.subList(finalI, finalJ + 1)).size();
                    Set<DescriptionMatch> intersectedCIDsWithScores = intersectedDescriptionMatches.stream()
                            .peek(descriptionMatch -> descriptionMatch.computeScore(numOfDistinctMatchingTokens))
                            .collect(Collectors.toSet());

                    // store matrix cells
                    cells.put(tokenSubsequence, intersectedCIDsWithScores);
                });
                futureList.add(future);
            }
        }
        joinAndClearFutures(futureList);
    }

    private void joinAndClearFutures(List<Future<?>> futureList){
        futureList.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        futureList.clear();
    }

    private void filterTopMatches(double score) {
        final int maxAmount = 64;
        topScoredDescriptions = cells.keySet().stream()
                .map(cells::get)
                .flatMap(Set::stream)
                .sorted(Comparator.comparingDouble(DescriptionMatch::getScore).reversed())
                .limit(maxAmount)
                .filter(did -> did.getScore() > score)
                .collect(Collectors.toList());
    }

    private void clearMatrix(){
        cells.clear();
        topScoredDescriptions.clear();
    }

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    public Set<DescriptionMatch> getDescriptionsContainingSingleToken(String token) {
        Query containsTokenQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.wildcardQuery(TERM_FOLDED, "* " + token + " *"))
                .withFields(DESCRIPTION_ID, TERM)
                .build();
        Query startsWithTokenQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.wildcardQuery(TERM_FOLDED, token + " *"))
                .withFields(DESCRIPTION_ID, TERM)
                .build();
        Query endsWithTokenQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.wildcardQuery(TERM_FOLDED, "* " + token))
                .withFields(DESCRIPTION_ID, TERM)
                .build();
        Query directMatchQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.matchQuery(TERM_FOLDED, token))
                .withFields(DESCRIPTION_ID, TERM)
                .build();
        List<Query> queries = List.of(containsTokenQuery, startsWithTokenQuery, endsWithTokenQuery, directMatchQuery);

        List<SearchHits<Description>> searchHitsPerQuery = elasticsearchOperations.multiSearch(
                queries, Description.class, IndexCoordinates.of(DESCRIPTION_INDEX)
        );

        return searchHitsPerQuery.stream()
                .map(SearchHits::getSearchHits)
                .flatMap(Collection::stream)
                .map(SearchHit::getContent)
                .map(DescriptionMatch::new)
                .peek(dIdContainer -> dIdContainer.computeScore(1))
                .collect(Collectors.toSet());
    }

    public List<String> generateMatchingDescriptions(String input, double threshold){
        List<String> tokens = InputTokenizer.tokenize(input);
        clearMatrix();
        generateLexicon(tokens);
        filterTopMatches(threshold);
        List<DescriptionMatch> topMatches = getTopScoredDescriptions();

        return topMatches.stream()
                .map(DescriptionMatch::getDescriptionId)
                .collect(Collectors.toList());
    }

}

