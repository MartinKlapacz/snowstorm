package org.snomed.snowstorm.snomedConverter.queryclient;

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

import java.io.IOException;
import java.util.List;

public class AveliosMappingClient{

    RestHighLevelClient client;
    private final String INDEX_NAME = "avelios-mapping";

    public AveliosMappingClient(String host, int port) {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(host, port));
        this.client = new ESRestHighLevelClient(restClientBuilder);
    }

    public List<SearchHit> aveliosNameForSCTID(String aveliosName){
        return getMappingValue("name", aveliosName);
    }

    public List<SearchHit> sctidForAveliosName(String sctid){
        return getMappingValue("SCTID", sctid);
    }

    private List<SearchHit> getMappingValue(String fieldName, String fieldValue){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery(fieldName, fieldValue).fuzziness(Fuzziness.ZERO));

        String[] includeFields = new String[]{"name", "SCTID"};
        searchSourceBuilder.fetchSource(includeFields, new String[]{});

        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.source(searchSourceBuilder);
        SearchResponse response;
        try {
            response = client.search(searchRequest, RequestOptions.DEFAULT);
            return List.of(response.getHits().getHits());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        AveliosMappingClient client = new AveliosMappingClient("localhost", 9200);
        List<SearchHit> searchHits = client.sctidForAveliosName("102002");
        searchHits.forEach(System.out::println);

//        List<SearchHit> searchHits = client.aveliosNameForSCTID("beta-2-Glykoprotein");
        searchHits.forEach(System.out::println);
    }
}
