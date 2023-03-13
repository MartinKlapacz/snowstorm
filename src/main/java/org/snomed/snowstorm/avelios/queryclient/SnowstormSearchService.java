package org.snomed.snowstorm.avelios.queryclient;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import org.snomed.snowstorm.config.Config;
import org.snomed.snowstorm.core.data.domain.ConceptMini;
import org.snomed.snowstorm.core.data.domain.ConceptView;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.data.domain.Relationship;
import org.snomed.snowstorm.core.data.services.DescriptionService;
import org.snomed.snowstorm.core.data.services.RelationshipService;
import org.snomed.snowstorm.core.data.services.ServiceException;
import org.snomed.snowstorm.rest.ConceptController;
import org.snomed.snowstorm.rest.DescriptionController;
import org.snomed.snowstorm.rest.pojo.BrowserDescriptionSearchResult;
import org.snomed.snowstorm.rest.pojo.ConceptSearchRequest;
import org.snomed.snowstorm.rest.pojo.InboundRelationshipsResult;
import org.snomed.snowstorm.rest.pojo.ItemsPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SnowstormSearchService {


    public static int count = 0;
    private final String BRANCH = "MAIN";
    private final int OFF_SET = 0;
    private final int LIMIT = 1000;
    private final String ACCEPT_LANGUAGE_HEADER = "en";

    @Autowired
    private ConceptController conceptController;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private DescriptionController descriptionController;

    public ConceptView findConcept(String conceptId) {
        return conceptController.findBrowserConcept(BRANCH, conceptId, Relationship.CharacteristicType.inferred, Config.DEFAULT_ACCEPT_LANG_HEADER);
    }

    public List<String> findConceptChildren(Collection<String> conceptIds) {
        String findChildrenECL = conceptIds.stream().map(id -> "<<! " + id).collect(Collectors.joining(" OR "));
        return findConceptsByECL(conceptIds, findChildrenECL);
    }

    public List<String> findConceptParents(Collection<String> conceptIds) {
        String findParentsECL = conceptIds.stream().map(id -> ">>! " + id).collect(Collectors.joining(" OR "));
        return findConceptsByECL(conceptIds, findParentsECL);
    }

    public List<String> findConceptDescendants(Collection<String> conceptIds) {
        String findParentsECL = conceptIds.stream().map(id -> "<< " + id).collect(Collectors.joining(" OR "));
        return findConceptsByECL(conceptIds, findParentsECL);
    }

    public List<String> findConceptAncestors(String conceptId) {
        return findConceptAncestors(Set.of(conceptId));
    }
    public List<String> findConceptAncestors(Collection<String> conceptIds) {
        return findConceptAncestors(conceptIds, 50);
    }
    public List<String> findConceptAncestors(Collection<String> conceptIds, Integer limit) {
        String findParentsECL = conceptIds.stream().map(id -> ">> " + id).collect(Collectors.joining(" OR "));
        return findConceptsByECL(conceptIds, findParentsECL, limit);
    }

    public List<String> filterPatientsWithPredecessorConcept(String targetConcept, Map<String, List<String>> patientIdToConceptsMap ) {
        List<String> matchingPatientIds = new ArrayList<>();
        for (String patientId: patientIdToConceptsMap.keySet()){
            List<String> patientConcepts = patientIdToConceptsMap.get(patientId);
            List<String> allAncestors = findConceptAncestors(patientConcepts, 10000);
            if (allAncestors.contains(targetConcept)) {
                matchingPatientIds.add(patientId);
            }
        }
        return matchingPatientIds;
    }



    private List<String> findConceptsByECL(Collection<String> conceptIds, String ecl) {
        return findConceptsByECL(conceptIds, ecl, null);
    }

    private List<String> findConceptsByECL(Collection<String> conceptIds, String ecl, Integer limit) {
        ConceptSearchRequest conceptSearchRequest = new ConceptSearchRequest();
        conceptSearchRequest.setTermActive(true);
        conceptSearchRequest.setEclFilter(ecl);
        conceptSearchRequest.setReturnIdOnly(true);
        conceptSearchRequest.setLimit(limit);

        ItemsPage<?> parentsItemPage = conceptController.search("MAIN", conceptSearchRequest, Config.DEFAULT_ACCEPT_LANG_HEADER, false);
        return parentsItemPage.getItems().stream()
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    public Set<ConceptMini> browserDescriptionSearch(String textInput) {
        Page<BrowserDescriptionSearchResult> browserDescriptionSearchResults = descriptionController.findBrowserDescriptions(
                BRANCH, textInput, true, null,
                null, null, null, null, null, null,
                null, true, null, true,
                DescriptionService.SearchMode.STANDARD, OFF_SET, LIMIT, ACCEPT_LANGUAGE_HEADER);

        return browserDescriptionSearchResults.getContent().stream()
                .map(BrowserDescriptionSearchResult::getConcept)
                .collect(Collectors.toSet());
    }

    public Collection<ConceptMini> findConceptChildren(String conceptId) {
        try {
            return conceptController.findConceptChildren(BRANCH, conceptId, Relationship.CharacteristicType.inferred,
                    false, Config.DEFAULT_ACCEPT_LANG_HEADER);
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Optional<Relationship> findRelationship(String relationshipId) {
        return Optional.ofNullable(relationshipService.findRelationship(BRANCH, relationshipId));
    }

    public List<Relationship> findRelationships(RelationshipQueryOptions options) {
        Page<Relationship> relationshipPage = relationshipService.findRelationships(
                BRANCH, options.getRelationshipId(),
                true,
                null,
                null,
                options.getSourceConcept(),
                null,
                options.getDestinationConcept(),
                null,
                null,
                options.getPageRequest()
        );
        return relationshipPage.getContent();
    }


    public List<Relationship> findInboundRelationships(String conceptId) {
        List<Relationship> inboundRelationships = relationshipService.findInboundRelationships(conceptId, BranchPathUriUtil.decodePath(BRANCH), null).getContent();
        return new InboundRelationshipsResult(inboundRelationships).getInboundRelationships();
    }

    public Description findDescription(String descriptionId) {
        return descriptionController.fetchDescription(BRANCH, descriptionId);
    }
}
