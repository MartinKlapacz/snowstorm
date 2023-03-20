package org.snomed.snowstorm.avelios;

import com.google.gson.Gson;
import org.elasticsearch.search.SearchHit;
import org.snomed.snowstorm.avelios.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.avelios.queryclient.AveliosMappingService;
import org.snomed.snowstorm.avelios.queryclient.SnowstormSearchService;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/convert", produces = "application/json")
public class AveliosConverterController {

    @Autowired
    TokenMatchMatrixService tokenMatchMatrixService;

    @Autowired
    SnowstormSearchService snowstormSearchService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    AveliosMappingService aveliosMappingService;

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

    @GetMapping(value = "mapSnomedToICD10/{sctId}")
    public String mapSnomedToICD10(@PathVariable String sctId) {
        return null;
    }


    @GetMapping(value = "filterPatients/{targetConceptIds}")
    public ResponseEntity<List<String>> filterPatientsWithMatchingConcepts(@PathVariable String targetConceptIds, @RequestBody Map<String, Set<String>> patientIdToConceptIds){
        List<String> targetConceptIdList = Arrays.asList(targetConceptIds.split(","));
        List<String> matchingPatients = snowstormSearchService.filterPatientsWithPredecessorConcept(targetConceptIdList, patientIdToConceptIds);
        return new ResponseEntity<>(matchingPatients, HttpStatus.OK);
    }

    @GetMapping(value = "ancestors/{conceptId}")
    public ResponseEntity<Set<String>> getAncestors(@PathVariable String conceptId) {
        Set<String> ancestorIds = snowstormSearchService.findConceptAncestors(conceptId);
        return new ResponseEntity<>(ancestorIds, HttpStatus.OK);
    }

    @GetMapping(value = "icd10ToSctId/{icd10}")
    public ResponseEntity<List<String>> getSctIdForIcd10(@PathVariable String icd10) {
        List<SearchHit> searchHits = aveliosMappingService.sctIdForIcd10(icd10);
        List<String> sctIdList = searchHits.stream()
                .map(SearchHit::getSourceAsMap)
                .map(map -> map.get("sctId"))
                .map(obj -> (String) obj)
                .collect(Collectors.toList());
        return new ResponseEntity<>(sctIdList, HttpStatus.OK);
    }
}
