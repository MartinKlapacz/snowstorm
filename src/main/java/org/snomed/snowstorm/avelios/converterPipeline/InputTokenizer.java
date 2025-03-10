package org.snomed.snowstorm.avelios.converterPipeline;

import java.util.*;
import java.util.stream.Collectors;

public class InputTokenizer {

    private static final Set<String> wordsToIgnore = new HashSet<>();

    static {
        // todo: blacklist tokens
        wordsToIgnore.add("and");
    }

    public static List<String> tokenize(String input) {
        List<String> tokenList = Arrays.stream(input.split(" "))
                .filter(word -> !wordsToIgnore.contains(word))
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (tokenList.size() == 1){
            return tokenList;
        }

        List<String> tokenListNoEqualNeighbours = new ArrayList<>(tokenList.size());
        for (int i = 0; i < tokenList.size(); i++) {
            tokenListNoEqualNeighbours.add(tokenList.get(i));
            if (i != tokenList.size() - 1 && tokenList.get(i).equals(tokenList.get(i + 1))) {
                // skip if current token is equal to neighbour
                i++;
            }
        }
        return tokenListNoEqualNeighbours;
    }
}
