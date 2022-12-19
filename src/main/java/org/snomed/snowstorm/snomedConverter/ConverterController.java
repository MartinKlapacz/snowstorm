package org.snomed.snowstorm.snomedConverter;

import com.google.gson.Gson;
import org.snomed.snowstorm.core.data.domain.Concept;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.data.repositories.ConceptRepository;
import org.snomed.snowstorm.core.data.repositories.DescriptionRepository;
import org.snomed.snowstorm.snomedConverter.converterPipeline.DIdContainer;
import org.snomed.snowstorm.snomedConverter.converterPipeline.InputTokenizer;
import org.snomed.snowstorm.snomedConverter.converterPipeline.TokenMatchingMatrix;
import org.snomed.snowstorm.snomedConverter.queryclient.QueryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/converter", produces = "application/json")
public class ConverterController {

    @Autowired
    TokenMatchingMatrix tokenMatchingMatrix;

    @Autowired
    QueryClient queryClient;
    @Autowired
    private DescriptionRepository descriptionRepository;
    @Autowired
    private ConceptRepository conceptRepository;

    @GetMapping(value = "/{input}")
    public String convert(@PathVariable String input) {
        List<String> tokens = InputTokenizer.tokenize(input);

        tokenMatchingMatrix.clearMatrix();
        tokenMatchingMatrix.generateLexicon(tokens);
        List<DIdContainer> topMatches = tokenMatchingMatrix.getTopMatchingDescriptionIds(0.5);

        List<Concept> matchingConcepts = topMatches.stream().map(DIdContainer::getDescriptionId)
                .map(descriptionRepository::findDescriptionByDescriptionId)
                .flatMap(Collection::stream)
                .map(Description::getConceptId)
                .map(conceptRepository::findConceptByConceptId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return new Gson().toJson(matchingConcepts);
    }
}
