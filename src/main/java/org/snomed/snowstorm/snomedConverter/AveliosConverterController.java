package org.snomed.snowstorm.snomedConverter;

import com.google.gson.Gson;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.snomedConverter.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.snomedConverter.queryclient.SnowstormSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping(value = "/convert", produces = "application/json")
public class AveliosConverterController {

    @Autowired
    TokenMatchMatrixService tokenMatchMatrixService;

    @Autowired
    SnowstormSearchService snowstormSearchService;

    @Autowired
    ConceptService conceptService;

    @Value("${avelios.converter.threshold:0.5}")
    double threshold;

    @GetMapping(value = "/{input}")
    public String mapTextToSnomedCTConcepts(@PathVariable String input) {
        Set<String> descriptionId = tokenMatchMatrixService.generateMatchingDescriptions(input, threshold);
        Set<String> conceptIds = conceptService.findConceptsByDescriptionIds(descriptionId);

        Set<String> parentIds = snowstormSearchService.findConceptParents(conceptIds);
        Set<String> childrenIds = snowstormSearchService.findConceptChildren(conceptIds);
        Set<String> ancestorIds = snowstormSearchService.findConceptAncestors(conceptIds);
        Set<String> descendantIds = snowstormSearchService.findConceptDescendants(conceptIds);

        return new Gson().toJson(descriptionId);
    }

    @GetMapping(value = "commonAncestors/{input1}/{input2}")
    public String getCommonAncestors(@PathVariable String input1, @PathVariable String input2) {
        Set<String> conceptIds1 = tokenMatchMatrixService.generateMatchingDescriptions(input1, threshold);
        Set<String> conceptIds2 = tokenMatchMatrixService.generateMatchingDescriptions(input2, threshold);

        Set<String> ancestorIds1 = snowstormSearchService.findConceptAncestors(conceptIds1);
        Set<String> ancestorIds2 = snowstormSearchService.findConceptAncestors(conceptIds2);
        ancestorIds1.retainAll(ancestorIds2);
        return new Gson().toJson(ancestorIds1);
    }
}
