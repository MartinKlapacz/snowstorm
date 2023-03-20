package org.snomed.snowstorm.avelios.queryclient;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.elasticsearch.core.ESRestHighLevelClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class AveliosMappingService {

    RestHighLevelClient client;

    public AveliosMappingService() {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost("localhost", 9200));
        this.client = new ESRestHighLevelClient(restClientBuilder);
    }

    public List<SearchHit> aveliosNameForSCTID(String aveliosName) {
        return getMappingValue("name", aveliosName, "avelios-mapping");
    }

    public List<SearchHit> sctIdForAveliosName(String sctId) {
        return getMappingValue("SCTID", sctId, "avelios-mapping");
    }

    public List<SearchHit> sctIdForIcd10(String icd10) {
        return getMappingValue("icd10Code", icd10, "sct-to-icd10");
    }

    private List<SearchHit> getMappingValue(String fieldName, String fieldValue, String indexName) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery(fieldName, fieldValue).fuzziness(Fuzziness.ZERO));

//        String[] includeFields = new String[]{"name", "SCTID"};
        String[] includeFields = new String[]{"sctId", "icd10Code" };
        searchSourceBuilder.fetchSource(includeFields, new String[]{});

        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);
        SearchResponse response;
        try {
            response = client.search(searchRequest, RequestOptions.DEFAULT);
            return List.of(response.getHits().getHits());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
