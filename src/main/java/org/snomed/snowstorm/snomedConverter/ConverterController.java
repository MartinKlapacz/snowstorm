package org.snomed.snowstorm.snomedConverter;

import com.google.gson.Gson;
import org.snomed.snowstorm.core.data.repositories.ConceptRepository;
import org.snomed.snowstorm.core.data.repositories.DescriptionRepository;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.rest.ConceptController;
import org.snomed.snowstorm.snomedConverter.converterPipeline.DescriptionMatch;
import org.snomed.snowstorm.snomedConverter.converterPipeline.InputTokenizer;
import org.snomed.snowstorm.snomedConverter.converterPipeline.TokenMatchingMatrix;
import org.snomed.snowstorm.snomedConverter.queryclient.SnowstormEndpointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/converter", produces = "application/json")
public class ConverterController {

    @Autowired
    TokenMatchingMatrix tokenMatchingMatrix;

    @Autowired
    SnowstormEndpointsService snowstormEndpointsService;
    @Autowired
    DescriptionRepository descriptionRepository;
    @Autowired
    ConceptRepository conceptRepository;
    @Autowired
    ConceptController conceptController;

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

        Set<String> parentIds = snowstormEndpointsService.findConceptParents(conceptIds);
        Set<String> childrenIds = snowstormEndpointsService.findConceptChildren(conceptIds);
        Set<String> ancestorIds = snowstormEndpointsService.findConceptAncestors(conceptIds);
        Set<String> decendantIds = snowstormEndpointsService.findConceptDescendants(conceptIds);

        return new Gson().toJson(matchingDescriptionIds);
    }

    @GetMapping(value = "commonAncestors/{input1}/{input2}")
    public String convertDouble(@PathVariable String input1, @PathVariable String input2) {
        List<String> tokens = InputTokenizer.tokenize(input1);

        tokenMatchingMatrix.clearMatrix();
        tokenMatchingMatrix.generateLexicon(tokens);
        tokenMatchingMatrix.filterTopMatches(threshold);
        List<DescriptionMatch> topMatches1 = tokenMatchingMatrix.getTopScoredDescriptions();

        List<String> matchingDescriptionIds1 = topMatches1.stream()
                .map(DescriptionMatch::getDescriptionId)
                .collect(Collectors.toList());

        List<String> conceptIds1 = conceptService.findConceptsByDescriptionIds(matchingDescriptionIds1);


        tokenMatchingMatrix.clearMatrix();
        tokenMatchingMatrix.generateLexicon(tokens);
        tokenMatchingMatrix.filterTopMatches(threshold);
        List<DescriptionMatch> topMatches2 = tokenMatchingMatrix.getTopScoredDescriptions();

        List<String> matchingDescriptionIds2 = topMatches2.stream()
                .map(DescriptionMatch::getDescriptionId)
                .collect(Collectors.toList());

        List<String> conceptIds2 = conceptService.findConceptsByDescriptionIds(matchingDescriptionIds2);


        Set<String> ancestorIds = snowstormEndpointsService.findConceptAncestors(conceptIds1);
        Set<String> ancestorIds2 = snowstormEndpointsService.findConceptAncestors(conceptIds2);

        ancestorIds.retainAll(ancestorIds2);

        return new Gson().toJson(ancestorIds);
    }
}
