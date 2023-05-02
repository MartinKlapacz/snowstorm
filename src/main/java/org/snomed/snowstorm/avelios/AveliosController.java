package org.snomed.snowstorm.avelios;

import org.snomed.snowstorm.avelios.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.avelios.queryclient.SnowstormOperationService;
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
    public ResponseEntity<List<String>> filterPatientsWithMatchingConcepts(@PathVariable String targetConceptIds, @RequestBody Map<String, Set<String>> patientData){
        List<String> targetConceptIdList = Arrays.asList(targetConceptIds.split(","));
        List<String> matchingPatients = snowstormOperationService.filterPatientsWithPredecessorConcept(targetConceptIdList, patientData);
        return new ResponseEntity<>(matchingPatients, HttpStatus.OK);
    }

    @GetMapping(value = "ancestors/{conceptId}")
    public ResponseEntity<Set<String>> getAncestors(@PathVariable String conceptId) {
        Set<String> ancestorIds = snowstormOperationService.findConceptAncestors(conceptId);
        return new ResponseEntity<>(ancestorIds, HttpStatus.OK);
    }


    @PostMapping(value = "publish")
    public ResponseEntity<String> publishFinishedTreatmentData(
            @RequestParam String patientId,
            @RequestParam String treatmentId,
            @RequestParam String visitId,
            @RequestBody Map<String, List<String>> body) {

        for (String methodIdentifier: body.keySet()) {
            snowstormOperationService.saveSnomedCtDataForTreatmentByMethod(
                    patientId,
                    treatmentId,
                    visitId,
                    body.get(methodIdentifier),
                    TranslationMethod.valueOf(methodIdentifier)
            );
        }
        return ResponseEntity.ok("success");
    }

    @GetMapping(value = "findTreatments")
    public ResponseEntity<List<Map<String, String>>> findTreatmentsWithSnomedCt(@RequestParam String targetSctIds, @RequestParam String translationMethods) {
        List<String> targetSctIdList = Arrays.asList(targetSctIds.split(","));
        List<TranslationMethod> translationMethodList = Arrays.asList(translationMethods.split(",")).stream()
                .map(TranslationMethod::valueOf)
                .collect(Collectors.toList());
        List<Map<String, String>> patientTreatmentFinishList = snowstormOperationService.findTreatmentsWithSnomedCt(
                targetSctIdList,
                translationMethodList
        );
        return ResponseEntity.ok(patientTreatmentFinishList);
    }
}
