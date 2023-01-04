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
    private final Map<List<String>, Set<DIdContainer>> cells = new ConcurrentHashMap<>();
    @Autowired
    AugmentedLexiconService augmentedLexiconService;
    @Autowired
    ConceptRepository conceptRepository;
    @Autowired
    DescriptionController descriptionController;
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    @Getter
    private List<DIdContainer> topMatchingDescriptions = new ArrayList<>();

    public void generateLexicon(List<String> tokens) {
        Map<String, Set<DIdContainer>> tokenToDescriptionIdsMap = new ConcurrentHashMap<>();

        int numOfCells = tokens.size() + tokens.size() * (tokens.size() - 1) / 2;
        List<Future<?>> futureList = new ArrayList<>(numOfCells);

        // for each token get the descriptionIds/wordCounts of the descriptions that contain the token
        for (String token : tokens) {
            Future<?> future = executorService.submit(() -> {
                Set<DIdContainer> conceptSet = augmentedLexiconService.getConceptsContainingToken(token);
                tokenToDescriptionIdsMap.put(token, conceptSet);
                cells.put(List.of(token), conceptSet);
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
                    List<Set<DIdContainer>> dIdAndSizeSetsToIntersect = tokenSubsequence.stream()
                            .map(tokenToDescriptionIdsMap::get)
                            .collect(Collectors.toList());

                    // compute their intersection
                    Set<DIdContainer> intersectedDIdAndSizeSet = new HashSet<>(dIdAndSizeSetsToIntersect.get(0));
                    dIdAndSizeSetsToIntersect.stream().skip(1).forEach(intersectedDIdAndSizeSet::retainAll);


                    // use the description size to compute the id scores
                    int tokenSubsequenceLength = String.join(" ", tokenSubsequence).length();
                    Set<DIdContainer> intersectedCIDsWithScores = intersectedDIdAndSizeSet.stream()
                            .peek(dIdContainer -> dIdContainer.computeScore(tokenSubsequenceLength))
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

    public void collectTopMatchingDescriptionIds(double threshold) {
        topMatchingDescriptions = cells.keySet().stream()
                .map(cells::get)
                .flatMap(Set::stream)
                .filter(dIdContainer -> dIdContainer.getScore() >= threshold)
                .sorted(DIdContainer.COMPARATOR.reversed())
                .collect(Collectors.toList());
    }

    public void clearMatrix(){
        cells.clear();
        topMatchingDescriptions.clear();
    }

}

