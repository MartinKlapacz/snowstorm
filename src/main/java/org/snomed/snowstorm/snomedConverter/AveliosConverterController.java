package org.snomed.snowstorm.snomedConverter;

import com.google.gson.Gson;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.rest.BranchController;
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

    @Autowired
    BranchController branchController;

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


    @GetMapping(value = "/deleteUnnecessaryBranches")
    public String deleteUnnecessaryBranches() {
        Set<String> idsOfBranchesToDelete = Set.of(
                "308916002", // Environment or geographical location
                "272379006", // Event, Organism (organism)
                "410607006", // Organism (organism)
                "373873005", // Pharmaceutical / biologic product (product)
                "260787004", // Physical object (physical object)
//                "71388002",  // Procedure (procedure)
                "419891008", // Record artifact (record artifact)
                "243796009", // Situation with explicit context (situation)
                "900000000000441003", // SNOMED CT Model Component (metadata)
                "48176007", // Social context (social concept)
                "370115009", // Special concept (special concept)
                "254291000" //  Staging and scales (staging scale)
        );

        branchController.unlockBranch("MAIN");
        for (String branchId: idsOfBranchesToDelete){
            Set<String> conceptIdsToDelete = snowstormSearchService.findConceptDescendants(branchId);
            conceptIdsToDelete.add(branchId);
            conceptService.deleteConceptAndComponents(conceptIdsToDelete, "MAIN", true);
        }
        branchController.lockBranch("MAIN", "lock after unlocking for deleting concepts");
        return new Gson().toJson("success");
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
