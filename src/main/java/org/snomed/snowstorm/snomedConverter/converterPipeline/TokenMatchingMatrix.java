package org.snomed.snowstorm.snomedConverter.converterPipeline;

import lombok.Getter;
import org.snomed.snowstorm.core.data.repositories.ConceptRepository;
import org.snomed.snowstorm.rest.DescriptionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TokenMatchingMatrix {
    private final Map<List<String>, Set<DescriptionMatch>> cells = new ConcurrentHashMap<>();
    @Autowired
    AugmentedLexiconService augmentedLexiconService;
    @Autowired
    ConceptRepository conceptRepository;
    @Autowired
    DescriptionController descriptionController;
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    @Getter
    private List<DescriptionMatch> topScoredDescriptions = new ArrayList<>();


    public void generateLexicon(List<String> tokens) {
        Map<String, Set<DescriptionMatch>> tokenToDescriptionMatches = new ConcurrentHashMap<>();

        int numOfCells = tokens.size() + tokens.size() * (tokens.size() - 1) / 2;
        List<Future<?>> futureList = new ArrayList<>(numOfCells);

        // for each token get the descriptionIds/wordCounts of the descriptions that contain the token
        for (String token : tokens) {
            Future<?> future = executorService.submit(() -> {
                Set<DescriptionMatch> descriptionMatchSet = augmentedLexiconService.getDescriptionsContainingSingleToken(token);
                tokenToDescriptionMatches.put(token, descriptionMatchSet);
                cells.put(List.of(token), descriptionMatchSet);
            });
            futureList.add(future);
        }
        joinAndClearFutures(futureList);


        for (int i = 0; i < tokens.size(); i++) {
            for (int j = i + 1; j < tokens.size(); j++) {
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
                            .map(tokenToDescriptionMatches::get)
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

    public void filterTopMatches(double score) {
        final int maxAmount = 64;
        topScoredDescriptions = cells.keySet().stream()
                .map(cells::get)
                .flatMap(Set::stream)
                .sorted(Comparator.comparingDouble(DescriptionMatch::getScore).reversed())
                .limit(maxAmount)
                .filter(did -> did.getScore() > score)
                .collect(Collectors.toList());
    }

    public void clearMatrix(){
        cells.clear();
        topScoredDescriptions.clear();
    }

}

