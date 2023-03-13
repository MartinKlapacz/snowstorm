package org.snomed.snowstorm.avelios;

import com.google.gson.Gson;
import org.snomed.snowstorm.avelios.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.avelios.queryclient.SnowstormSearchService;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        List<String> descriptionId = tokenMatchMatrixService.generateMatchingDescriptions(input, threshold);
        List<String> conceptIds = conceptService.findConceptsByDescriptionIds(descriptionId);

        List<String> parentIds = snowstormSearchService.findConceptParents(conceptIds);
        List<String> childrenIds = snowstormSearchService.findConceptChildren(conceptIds);
        List<String> ancestorIds = snowstormSearchService.findConceptAncestors(conceptIds);
        List<String> descendantIds = snowstormSearchService.findConceptDescendants(conceptIds);

        return new Gson().toJson(descriptionId);
    }

    @GetMapping(value = "commonAncestors/{input1}/{input2}")
    public String getCommonAncestors(@PathVariable String input1, @PathVariable String input2) {
        List<String> conceptIds1 = tokenMatchMatrixService.generateMatchingDescriptions(input1, threshold);
        List<String> conceptIds2 = tokenMatchMatrixService.generateMatchingDescriptions(input2, threshold);

        List<String> ancestorIds1 = snowstormSearchService.findConceptAncestors(conceptIds1);
        List<String> ancestorIds2 = snowstormSearchService.findConceptAncestors(conceptIds2);
        ancestorIds1.retainAll(ancestorIds2);
        return new Gson().toJson(ancestorIds1);
    }

    @GetMapping(value = "mapSnomedToICD10/{sctId}")
    public String mapSnomedToICD10(@PathVariable String sctId) {
        return null;
    }


    @GetMapping(value = "filterPatients/{targetConceptId}")
    public ResponseEntity<List<String>> filterPatientsWithMatchingConcepts(@PathVariable String targetConceptId, @RequestBody Map<String, List<String>> patientIdToConceptIds){
        List<String> matchingPatients = snowstormSearchService.filterPatientsWithPredecessorConcept(targetConceptId, patientIdToConceptIds);
        return new ResponseEntity<>(matchingPatients, HttpStatus.OK);
    }

    @GetMapping(value = "ancestors/{conceptId}")
    public ResponseEntity<List<String>> getAncestors(@PathVariable String conceptId) {
        List<String> ancestorIds = snowstormSearchService.findConceptAncestors(conceptId);
        return new ResponseEntity<>(ancestorIds, HttpStatus.OK);
    }

}
