package org.snomed.snowstorm.avelios.queryclient;

import lombok.SneakyThrows;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.snomed.snowstorm.avelios.SnomedCtDataForTreatment;
import org.snomed.snowstorm.avelios.TranslationMethod;
import org.snomed.snowstorm.avelios.converterPipeline.TokenMatchMatrixService;
import org.snomed.snowstorm.config.Config;
import org.snomed.snowstorm.core.data.domain.ConceptMini;
import org.snomed.snowstorm.core.data.domain.Relationship;
import org.snomed.snowstorm.core.data.services.ServiceException;
import org.snomed.snowstorm.rest.ConceptController;
import org.snomed.snowstorm.rest.pojo.ConceptSearchRequest;
import org.snomed.snowstorm.rest.pojo.ItemsPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.snomed.snowstorm.avelios.TranslationMethod.*;


@Service
public class SnowstormOperationService {

    public static int count = 0;
    private final String BRANCH = "MAIN";

    @Autowired
    private ConceptController conceptController;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    TokenMatchMatrixService matchMatrixService;


    public static final Map<TranslationMethod, String> METHOD_IDENTIFIER_TO_INDEX_NAME = Map.ofEntries(
            new AbstractMap.SimpleEntry<>(TRANSLATION_METHOD_RULE_BASED, "rule-based-collection"),
            new AbstractMap.SimpleEntry<>(TRANSLATION_METHOD_KNOWLEDGE_INPUT_MAPPING, "knowledge-input-mapping-collection"),
            new AbstractMap.SimpleEntry<>(TRANSLATION_METHOD_FUZZY_TOKEN_MATCHING, "fuzzy-collection"),
            new AbstractMap.SimpleEntry<>(TRANSLATION_METHOD_ICD10_MAPPING, "icd10-mapping-collection"),
            new AbstractMap.SimpleEntry<>(TRANSLATION_METHOD_ALPHAID_MAPPING, "alpha-id-mapping-collection"),
            new AbstractMap.SimpleEntry<>(TRANSLATION_METHOD_ORPHAID_MAPPING, "orpha-id-mapping-collection")
    );

    @PostConstruct
    private void setupSctIndexes() throws IOException {
        // make sure treatment data indexes exist
        for (String indexName: METHOD_IDENTIFIER_TO_INDEX_NAME.values()) {
            GetIndexRequest request = new GetIndexRequest(indexName);
            request.local(false);
            request.includeDefaults(false);

            if (!restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT)) {
                Settings settings = Settings.builder()
                        .put("index.number_of_shards", 1)
                        .put("index.number_of_replicas", 1)
                        .build();

                // Define the index mapping
                XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()
                        .startObject()
                        .startObject("properties")
                        .startObject("field1")
                        .field("type", "text")
                        .endObject()
                        .endObject()
                        .endObject();

                // Create the index request
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName)
                        .settings(settings)
                        .mapping(mappingBuilder);

                // Send the request and get the response
                restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            }
        }
    }

    public Set<String> findConceptAncestors(String conceptId) {
        return findConceptAncestors(Set.of(conceptId));
    }
    public Set<String> findConceptAncestors(Collection<String> conceptIds) {
        return findConceptAncestors(conceptIds, 50);
    }
    public Set<String> findConceptAncestors(Collection<String> conceptIds, Integer limit) {
        String findParentsECL = conceptIds.stream().map(id -> "> " + id).collect(Collectors.joining(" OR "));
        return findConceptsByECL(conceptIds, findParentsECL, limit);
    }

    public List<String> filterPatientsWithPredecessorConcept(List<String> targetConcept, Map<String, Set<String>> patientIdToConceptsMap ) {
        List<String> matchingPatientIds = new ArrayList<>();
        for (String patientId: patientIdToConceptsMap.keySet()){
            Set<String> patientConcepts = patientIdToConceptsMap.get(patientId);
            Set<String> allAncestors = findConceptAncestors(patientConcepts, 10000);
            if (allAncestors.containsAll(targetConcept)) {
                matchingPatientIds.add(patientId);
            }
        }
        return matchingPatientIds;
    }


    private Set<String> findConceptsByECL(Collection<String> conceptIds, String ecl, Integer limit) {
        ConceptSearchRequest conceptSearchRequest = new ConceptSearchRequest();
        conceptSearchRequest.setTermActive(true);
        conceptSearchRequest.setEclFilter(ecl);
        conceptSearchRequest.setReturnIdOnly(true);
        conceptSearchRequest.setLimit(limit);

        ItemsPage<?> parentsItemPage = conceptController.search("MAIN", conceptSearchRequest, Config.DEFAULT_ACCEPT_LANG_HEADER, false);
        return parentsItemPage.getItems().stream()
                .map(item -> (String) item)
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

    public void saveSnomedCtDataForTreatmentByMethod(String patientId, String treatmentId, String visitId, Collection<String> inputData, TranslationMethod translationMethod) {
        if (inputData.isEmpty()){
            return;
        }
        Set<String> directHits = new HashSet<>();

        switch (translationMethod) {
            case TRANSLATION_METHOD_RULE_BASED:
            case TRANSLATION_METHOD_KNOWLEDGE_INPUT_MAPPING:
            case TRANSLATION_METHOD_ICD10_MAPPING:
            case TRANSLATION_METHOD_ALPHAID_MAPPING:
            case TRANSLATION_METHOD_ORPHAID_MAPPING:
                // for these methods sctIds are already found
                directHits = new HashSet<>(inputData);
                break;
            case TRANSLATION_METHOD_FUZZY_TOKEN_MATCHING:
                for (String input: inputData) {
                    Map<String, Integer> matchingResult = matchMatrixService.generateMatchingDescriptions(input, 50);
                    directHits.addAll(matchingResult.keySet());
                }
                break;
            default:
                throw new RuntimeException("Invalid translation method");
        }
        var snomedCtDataForTreatment = new SnomedCtDataForTreatment(
                patientId,
                treatmentId,
                visitId,
                directHits,
                // precompute ancestors for future queries
                findConceptAncestors(directHits)
        );
        IndexQuery indexQuery = new IndexQueryBuilder().withObject(snomedCtDataForTreatment).build();
        elasticsearchOperations.index(indexQuery, IndexCoordinates.of(METHOD_IDENTIFIER_TO_INDEX_NAME.get(translationMethod)));
    }

    @SneakyThrows
    public List<Map<String, String>> findTreatmentsWithSnomedCt(List<String> targetSctIdList, List<TranslationMethod> translationMethods) {

        String[] indexesToSearch = translationMethods.stream()
                .map(METHOD_IDENTIFIER_TO_INDEX_NAME::get)
                .toArray(String[]::new);

        BoolQueryBuilder sctIdHitAncestorsBoolQueryBuilder = QueryBuilders.boolQuery();
        BoolQueryBuilder directSctIdHitsBoolQueryBuilder = QueryBuilders.boolQuery();


        for (String targetSctId : targetSctIdList) {
            QueryBuilder termQueryForAncestors = QueryBuilders.termQuery("sctIdHitAncestors", targetSctId);
            sctIdHitAncestorsBoolQueryBuilder.filter(termQueryForAncestors);

            QueryBuilder termQueryForDirectHits = QueryBuilders.termQuery("directSctIdHits", targetSctId);
            directSctIdHitsBoolQueryBuilder.filter(termQueryForDirectHits);
        }

        // Combine the queries using a should clause to represent OR
        BoolQueryBuilder combinedBoolQueryBuilder = QueryBuilders.boolQuery();
        combinedBoolQueryBuilder.should(sctIdHitAncestorsBoolQueryBuilder);
        combinedBoolQueryBuilder.should(directSctIdHitsBoolQueryBuilder);

        // Build the native search query with the bool query
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(combinedBoolQueryBuilder)
                .build();

        // Execute the query and retrieve the hits
        SearchHits<SnomedCtDataForTreatment> searchHits = elasticsearchOperations.search(
                searchQuery,
                SnomedCtDataForTreatment.class,
                IndexCoordinates.of(indexesToSearch)
        );


//         Convert the search hits into a list of SnomedCtDataForTreatment entities
        List<Map<String, String>> resultMap = searchHits.get()
                .map(SearchHit::getContent)
                .map(content -> Map.ofEntries(
                        new AbstractMap.SimpleEntry<>("patientId", content.getPatientId()),
                        new AbstractMap.SimpleEntry<>("treatmentId", content.getTreatmentId()),
                        new AbstractMap.SimpleEntry<>("visitId", content.getVisitId())
                ))
                .collect(Collectors.toList());
        return resultMap;
    }
}
