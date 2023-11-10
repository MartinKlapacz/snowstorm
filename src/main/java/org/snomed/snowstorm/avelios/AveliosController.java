package org.snomed.snowstorm.avelios;

import org.snomed.snowstorm.avelios.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.avelios.queryclient.SnowstormOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.snomed.snowstorm.avelios.TranslationMethod.TRANSLATION_METHOD_NONE;

@RestController
@RequestMapping(value = "/avelios", produces = "application/json")
public class AveliosController {

    @Autowired
    private TokenMatchMatrixService tokenMatchMatrixService;

    @Autowired
    private SnowstormOperationService snowstormOperationService;

    @Value("${avelios.converter.threshold}")
    double threshold;

    @GetMapping(value = "matchingMatrix/{input}")
    public ResponseEntity<Map<String, Integer>> mapTextToSnomedCTConcepts(@PathVariable String input) {
        Map<String, Integer> descriptionIdsAndScore
                = tokenMatchMatrixService.generateMatchingDescriptions(input, threshold);

        return new ResponseEntity<>(descriptionIdsAndScore, HttpStatus.OK);
    }

    @PostMapping(value = "filterPatients/{targetConceptIds}")
    public ResponseEntity<List<String>> filterPatientsWithMatchingConcepts(@PathVariable String targetConceptIds, @RequestBody Map<String, Set<String>> patientData) {
        List<String> targetConceptIdList = Arrays.asList(targetConceptIds.split(","));
        List<String> matchingPatients = snowstormOperationService.filterPatientsWithPredecessorConcept(targetConceptIdList, patientData);
        return new ResponseEntity<>(matchingPatients, HttpStatus.OK);
    }

    @PostMapping(value = "ancestors")
    public ResponseEntity<Map<String, Collection<String>>> getAncestorsPerMethod(@RequestBody Map<String, List<String>> body) {
        Map<String, Collection<String>> responseData = new HashMap<>();
        for (String methodIdentifier : body.keySet()) {
            TranslationMethod translationMethod = TranslationMethod.saveValueOf(methodIdentifier);
            if (translationMethod == TRANSLATION_METHOD_NONE) {
                return ResponseEntity.badRequest().build();
            }
            Collection<String> ancestorIds = snowstormOperationService.findConceptAncestors(body.get(methodIdentifier));
            responseData.put(translationMethod.toString(), ancestorIds);
        }
        return ResponseEntity.ok(responseData);
    }

    @GetMapping(value = "findTreatments")
    public ResponseEntity<List<Map<String, String>>> findTreatmentsWithSnomedCt(@RequestParam String targetSctIds, @RequestParam String translationMethods) {
        List<String> targetSctIdList = Arrays.asList(targetSctIds.split(","));
        List<TranslationMethod> translationMethodList = Arrays.asList(translationMethods.split(",")).stream()
                .map(TranslationMethod::saveValueOf)
                .collect(Collectors.toList());
        if (translationMethodList.contains(TRANSLATION_METHOD_NONE)) {
            return ResponseEntity.badRequest().build();
        }
        List<Map<String, String>> patientTreatmentFinishList = snowstormOperationService.findTreatmentsWithSnomedCt(
                targetSctIdList,
                translationMethodList
        );
        return ResponseEntity.ok(patientTreatmentFinishList);
    }
}
