package org.snomed.snowstorm.snomedConverter.converterPipeline;

import org.snomed.snowstorm.core.data.repositories.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TokenMatchingMatrix {
    private final Map<List<String>, Set<DIdContainer>> cells = new HashMap<>();
    @Autowired
    AugmentedLexiconService augmentedLexiconService;

    @Autowired
    ConceptRepository conceptRepository;

    public void generateLexicon(List<String> tokens) {
        Map<String, Set<DIdContainer>> tokenToDescriptionIdsMap = new HashMap<>();
        // for each token get the descriptionIds/wordCounts of the descriptions that contain the token
        for (String token : tokens) {
            Set<DIdContainer> dIdContainerSet = augmentedLexiconService.getDIDsForWord(token);
            tokenToDescriptionIdsMap.put(token, dIdContainerSet);
            cells.put(List.of(token), dIdContainerSet);
        }


        for (int i = 0; i < tokens.size(); i++) {
            for (int j = i + 1; j < tokens.size(); j++) {
                // get current token subsequence from indices
                List<String> tokenSubsequence = Arrays.stream(IntStream.rangeClosed(i, j).toArray())
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
            }
        }
    }

    public List<DIdContainer> getTopMatchingDescriptionIds(double threshold) {
        return cells.keySet().stream()
                .map(cells::get)
                .flatMap(Set::stream)
                .filter(dIdContainer -> dIdContainer.getScore() >= threshold)
                .sorted(DIdContainer.COMPARATOR.reversed())
                .collect(Collectors.toList());
    }

    public void clearMatrix(){
        cells.clear();
    }

}

