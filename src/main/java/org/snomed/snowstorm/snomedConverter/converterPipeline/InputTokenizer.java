package org.snomed.snowstorm.snomedConverter.converterPipeline;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InputTokenizer {

    private static final Set<String> wordsToIgnore = new HashSet<>();

    static {
        // todo: blacklist tokens
        wordsToIgnore.add("and");
    }

    public static List<String> tokenize(String input){
        return Arrays.stream(input.split(" "))
                .filter(word -> !wordsToIgnore.contains(word))
                .collect(Collectors.toList());
    }
}
