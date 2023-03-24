package org.snomed.snowstorm.avelios;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/avelios", produces = "application/json")
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

    private final Gson gson = new Gson();

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


    @GetMapping(value = "filterPatients/{targetConceptIds}/{sctData}")
    public ResponseEntity<List<String>> filterPatientsWithMatchingConcepts(@PathVariable String targetConceptIds, @PathVariable String sctData){
        List<String> targetConceptIdList = Arrays.asList(targetConceptIds.split(","));
        Type type = new TypeToken<Map<String, Set<String>>>(){}.getType();
        Map<String, Set<String>> patientIdToConceptIds = gson.fromJson(sctData, type);

        List<String> matchingPatients = snowstormSearchService.filterPatientsWithPredecessorConcept(targetConceptIdList, patientIdToConceptIds);
        return new ResponseEntity<>(matchingPatients, HttpStatus.OK);
    }

    @GetMapping(value = "ancestors/{conceptId}")
    public ResponseEntity<Set<String>> getAncestors(@PathVariable String conceptId) {
        Set<String> ancestorIds = snowstormSearchService.findConceptAncestors(conceptId);
        return new ResponseEntity<>(ancestorIds, HttpStatus.OK);
    }

    @GetMapping(value = "icd10ToSctId/{icd10}")
    public ResponseEntity<Map<String, List<String>>> getSctIdForIcd10(@PathVariable String icd10) {
        var searchHits = aveliosMappingService.sctIdForIcd10(icd10.split(","));
        return new ResponseEntity<>(searchHits, HttpStatus.OK);
    }
}
