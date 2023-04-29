package org.snomed.snowstorm.avelios;

import org.snomed.snowstorm.avelios.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.avelios.queryclient.AveliosMappingService;
import org.snomed.snowstorm.avelios.queryclient.SnowstormSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(value = "/avelios", produces = "application/json")
public class AveliosController {

    @Autowired
    private TokenMatchMatrixService tokenMatchMatrixService;

    @Autowired
    private SnowstormSearchService snowstormSearchService;

    @Autowired
    private AveliosMappingService aveliosMappingService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

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
        List<String> matchingPatients = snowstormSearchService.filterPatientsWithPredecessorConcept(targetConceptIdList, patientData);
        return new ResponseEntity<>(matchingPatients, HttpStatus.OK);
    }
    @GetMapping(value = "icd10ToSctId/{icd10}")
    public ResponseEntity<Map<String, List<String>>> getSctIdForIcd10(@PathVariable String icd10) {
        var searchHits = aveliosMappingService.sctIdForIcd10(icd10.split(","));
        return new ResponseEntity<>(searchHits, HttpStatus.OK);
    }

    @GetMapping(value = "ancestors/{conceptId}")
    public ResponseEntity<Set<String>> getAncestors(@PathVariable String conceptId) {
        Set<String> ancestorIds = snowstormSearchService.findConceptAncestors(conceptId);
        return new ResponseEntity<>(ancestorIds, HttpStatus.OK);
    }

    @GetMapping(value = "mapKnowledgeInputNamesToSctIds/{names}")
    public ResponseEntity<Map<String, Set<String>>> mapKnowledgeInputsToSctIds(@PathVariable String names) {
        List<String> nameArray = Arrays.asList(names.split(","));
        Map<String, Set<String>> conceptIds = aveliosMappingService.findSctIdsForKnowledgeInputNames(nameArray);
        return new ResponseEntity<>(conceptIds, HttpStatus.OK);
    }

    private static final Set<String> requiredBodyKeys = Set.of(
            "sctIdsFromRules",
            "knowledgeInputIds",
            "blockStringRepresentations"
    );

    @PostMapping(value = "publish")
    public ResponseEntity<String> publishFinishedTreatmentData(
            @RequestParam String patientId,
            @RequestParam String treatmentId,
            @RequestParam String visitId,
            @RequestBody Map<String, List<String>> body) {

        if (!body.keySet().containsAll(requiredBodyKeys)) {
            return ResponseEntity.badRequest().build();
        }

        List<String> sctIdsFromRules = body.get("sctIdsFromRules");
        List<String> knowledgeInputIds = body.get("knowledgeInputIds");
        List<String> blockStringRepresentations = body.get("blockStringRepresentations");

        var treatmentFinishSctResults = new SnomedCtDataForTreatment(
                patientId,
                treatmentId,
                visitId,
                sctIdsFromRules,
                new ArrayList<>(snowstormSearchService.findConceptAncestors(sctIdsFromRules))
        );

        IndexQuery indexQuery = new IndexQueryBuilder().withObject(treatmentFinishSctResults).build();
        String res = elasticsearchOperations.index(indexQuery, IndexCoordinates.of("test-index"));

        return ResponseEntity.ok(res);
    }
}
