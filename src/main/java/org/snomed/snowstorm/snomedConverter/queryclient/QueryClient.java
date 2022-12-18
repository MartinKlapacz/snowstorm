package org.snomed.snowstorm.snomedConverter.queryclient;

import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import org.snomed.snowstorm.config.Config;
import org.snomed.snowstorm.core.data.domain.ConceptMini;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.data.domain.Relationship;
import org.snomed.snowstorm.core.data.services.ConceptService;
import org.snomed.snowstorm.core.data.services.RelationshipService;
import org.snomed.snowstorm.core.data.services.ServiceException;
import org.snomed.snowstorm.rest.ConceptController;
import org.snomed.snowstorm.rest.DescriptionController;
import org.snomed.snowstorm.rest.pojo.InboundRelationshipsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QueryClient {


    private final int OFFSET_DEFAULT = 0;
    private final int LIMIT_DEFAULT = 50;

    private final String BRANCH = "MAIN";

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private ConceptController conceptController;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private DescriptionController descriptionController;

    public Optional<ConceptMini> findConcept(String conceptId) {
        return conceptController.findConcept(BRANCH, conceptId, Config.DEFAULT_ACCEPT_LANG_HEADER);
    }

    public Collection<ConceptMini> findConceptParents(String conceptId) {
        return conceptController.findConceptParents(BRANCH, conceptId, Relationship.CharacteristicType.inferred, false,
                Config.DEFAULT_ACCEPT_LANG_HEADER);
    }

    public Collection<ConceptMini> findConceptChildren(String conceptId){
        try{
            return conceptController.findConceptChildren(BRANCH, conceptId, Relationship.CharacteristicType.inferred,
                    false, Config.DEFAULT_ACCEPT_LANG_HEADER);
        } catch (ServiceException e) {
            return Collections.emptySet();
        }
    }

    public Optional<Relationship> findRelationship(String relationshipId){
        return Optional.ofNullable(relationshipService.findRelationship(BRANCH, relationshipId));
    }

    public List<Relationship> findRelationships(RelationshipQueryOptions options){
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

    public Description findDescription(String descriptionId){
        return descriptionController.fetchDescription(BRANCH, descriptionId);
    }
}
