package org.snomed.snowstorm.avelios.converterPipeline;

import org.elasticsearch.index.query.QueryBuilders;
import org.snomed.snowstorm.core.data.domain.Description;
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

@Service
public class TokenMatchMatrixService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final String DESCRIPTION_INDEX = "description";

    final int MAX_TOKEN_SUBSEQUENCE_LENGTH = 5;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    private Map<List<String>, Set<DescriptionMatch>> generateLexicon(List<String> tokens) {
        int numOfCells = tokens.size() + tokens.size() * (tokens.size() - 1) / 2;
        List<Future<?>> futureList = new ArrayList<>(numOfCells);
        final Map<List<String>, Set<DescriptionMatch>> cells = new ConcurrentHashMap<>();

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
        return cells;
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

    private List<DescriptionMatch> filterTopMatches(double score, int maxAmount, Map<List<String>, Set<DescriptionMatch>> cells) {
        return cells.keySet().stream()
                .map(cells::get)
                .flatMap(Set::stream)
                .sorted(Comparator.comparingDouble(DescriptionMatch::getScore).reversed())
                .limit(maxAmount)
                .filter(did -> did.getScore() > score)
                .collect(Collectors.toList());
    }

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

    public Map<String, Integer> generateMatchingDescriptions(String input, double threshold){
        final int maxAmount = 64;

        List<String> tokens = InputTokenizer.tokenize(input);
        Map<List<String>, Set<DescriptionMatch>> cells = generateLexicon(tokens);
        List<DescriptionMatch> topMatches = filterTopMatches(threshold, maxAmount, cells);

        return topMatches.stream().distinct().collect(Collectors.toMap(
                DescriptionMatch::getDescriptionId,
                DescriptionMatch::getScore
        ));
    }

}

