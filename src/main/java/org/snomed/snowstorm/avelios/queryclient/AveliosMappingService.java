package org.snomed.snowstorm.avelios.queryclient;

import com.google.gson.Gson;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filters;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.elasticsearch.search.aggregations.metrics.ParsedTopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ESRestHighLevelClient;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class AveliosMappingService {
    private final RestHighLevelClient client;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    public AveliosMappingService() {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost("localhost", 9200));
        this.client = new ESRestHighLevelClient(restClientBuilder);
    }

//    public List<SearchHit> aveliosNameForSCTID(String aveliosName) {
//        return getMappingValue("name", aveliosName, "avelios-mapping");
//    }
//
//    public List<SearchHit> sctIdForAveliosName(String sctId) {
//        return getMappingValue("SCTID", sctId, "avelios-mapping");
//    }

    public Map<String, List<String>> sctIdForIcd10(String... icd10Codes) {
        return getMappingValue("sct-to-icd10", "icd10Code", icd10Codes);
    }

    private Map<String, List<String>> getMappingValue(String targetIndexName, String targetColumnName, String... targetColumnValues) {
        // Create SearchSourceBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(0);

        // Create filters
        FiltersAggregator.KeyedFilter[] filters = new FiltersAggregator.KeyedFilter[targetColumnValues.length];
        for (int i = 0; i < targetColumnValues.length; i++) {
            FiltersAggregator.KeyedFilter filter = new FiltersAggregator.KeyedFilter(
                    targetColumnValues[i],
                    QueryBuilders.termQuery(targetColumnName, targetColumnValues[i])
            );
            filters[i] = filter;
        }
        var filtersAggregation = AggregationBuilders.filters("my_name", filters);

        var topHitsAggregation = AggregationBuilders.topHits("sctIds")
                .size(1000)
                .fetchSource(new String[]{"sctId"}, null);

        filtersAggregation.subAggregation(topHitsAggregation);
        searchSourceBuilder.aggregation(filtersAggregation);
        SearchRequest searchRequest = new SearchRequest(targetIndexName);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }

        List<? extends Filters.Bucket> buckets = ((ParsedFilters) searchResponse.getAggregations().getAsMap().get("my_name")).getBuckets();
        Map<String, List<String>> result = new HashMap<>();

        for (Filters.Bucket bucket : buckets) {
            String icd10Code = bucket.getKeyAsString();
            SearchHit[] aggregationHits = ((ParsedTopHits) bucket.getAggregations().get("sctIds")).getHits().getHits();
            List<String> sctIds = Arrays.stream(aggregationHits)
                    .map(SearchHit::getSourceAsMap)
                    .map(map -> map.get("sctId"))
                    .map(sctId -> (String) sctId)
                    .collect(Collectors.toList());
            result.put(icd10Code, sctIds);
        }

        return result;
    }
}
