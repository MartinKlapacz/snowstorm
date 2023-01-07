package org.snomed.snowstorm.snomedConverter;

import com.google.gson.Gson;
import org.snomed.snowstorm.core.data.repositories.ConceptRepository;
import org.snomed.snowstorm.core.data.repositories.DescriptionRepository;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.snomedConverter.converterPipeline.DescriptionMatch;
import org.snomed.snowstorm.snomedConverter.converterPipeline.InputTokenizer;
import org.snomed.snowstorm.snomedConverter.converterPipeline.TokenMatchingMatrix;
import org.snomed.snowstorm.snomedConverter.queryclient.SnowstormEndpointsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/converter", produces = "application/json")
public class ConverterController {

    @Autowired
    TokenMatchingMatrix tokenMatchingMatrix;

    @Autowired
    SnowstormEndpointsClient snowstormEndpointsClient;
    @Autowired
    DescriptionRepository descriptionRepository;
    @Autowired
    ConceptRepository conceptRepository;

    @Autowired
    ConceptService conceptService;

    @Value("${avelios.converter.threshold:0.5}")
    double threshold;

    @GetMapping(value = "/{input}")
    public String convert(@PathVariable String input) {
        List<String> tokens = InputTokenizer.tokenize(input);

        tokenMatchingMatrix.clearMatrix();
        tokenMatchingMatrix.generateLexicon(tokens);
        tokenMatchingMatrix.filterTopMatches(threshold);
        List<DescriptionMatch> topMatches = tokenMatchingMatrix.getTopScoredDescriptions();

        List<String> matchingDescriptionIds = topMatches.stream()
                .map(DescriptionMatch::getDescriptionId)
                .collect(Collectors.toList());

        List<String> conceptIds = conceptService.findConceptsByDescriptionIds(matchingDescriptionIds);

        // todo: use conceptIds ...

        return new Gson().toJson(matchingDescriptionIds);
    }
}
