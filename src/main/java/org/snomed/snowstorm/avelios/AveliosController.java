package org.snomed.snowstorm.avelios;

import com.google.gson.Gson;
import org.snomed.snowstorm.avelios.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.avelios.queryclient.AveliosMappingService;
import org.snomed.snowstorm.avelios.queryclient.SnowstormSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/avelios", produces = "application/json")
public class AveliosController {

    @Autowired
    TokenMatchMatrixService tokenMatchMatrixService;

    @Autowired
    SnowstormSearchService snowstormSearchService;

    @Autowired
    AveliosMappingService aveliosMappingService;

    @Value("${avelios.converter.threshold}")
    double threshold;

    @GetMapping(value = "matchingMatrix/{input}")
    public ResponseEntity<Map<String, Integer>> mapTextToSnomedCTConcepts(@PathVariable String input) {
        Map<String, Integer> descriptionIdsAndScore
                = tokenMatchMatrixService.generateMatchingDescriptions(input, threshold);

//        List<String> clinicalFindingIds = descriptionIdsAndScore.keySet().stream()
//                .filter(snowstormSearchService::isClinicalFining)
//                .collect(Collectors.toList());
//        descriptionIdsAndScore.keySet().retainAll(clinicalFindingIds);
        return new ResponseEntity<>(descriptionIdsAndScore, HttpStatus.OK);
    }


    @GetMapping(value = "mapSnomedToICD10/{sctId}")
    public String mapSnomedToICD10(@PathVariable String sctId) {
        return null;
    }


    @PostMapping(value = "filterPatients/{targetConceptIds}")
    public ResponseEntity<List<String>> filterPatientsWithMatchingConcepts(@PathVariable String targetConceptIds, @RequestBody Map<String, Set<String>> patientData){
        List<String> targetConceptIdList = Arrays.asList(targetConceptIds.split(","));
        List<String> matchingPatients = snowstormSearchService.filterPatientsWithPredecessorConcept(targetConceptIdList, patientData);
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
