package org.snomed.snowstorm.snomedConverter.converterPipeline;

import org.elasticsearch.index.query.QueryBuilders;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.data.repositories.DescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AugmentedLexiconService {

    private static final String DESCRIPTION_INDEX = "description";
    private static final String FIELD_NAME = "termFolded";
    @Autowired
    DescriptionRepository descriptionRepository;
    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    public Set<DescriptionMatch> getDescriptionsContainingSingleToken(String token) {
        Query containsTokenQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.wildcardQuery(FIELD_NAME, "* " + token + " *"))
                .build();
        Query startsWithTokenQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.wildcardQuery(FIELD_NAME, token + " *"))
                .build();
        Query endsWithTokenQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.wildcardQuery(FIELD_NAME, "* " + token))
                .build();
        Query directMatchQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.matchQuery(FIELD_NAME, token))
                .build();
        List<Query> queries = List.of(containsTokenQuery, startsWithTokenQuery, endsWithTokenQuery, directMatchQuery);

        List<SearchHits<Description>> searchHitsPerQuery = elasticsearchOperations.multiSearch(
                queries, Description.class, IndexCoordinates.of(DESCRIPTION_INDEX)
        );

        return searchHitsPerQuery.stream()
                .map(SearchHits::getSearchHits)
                .flatMap(Collection::stream)
                .map(SearchHit::getContent)
                .map(DescriptionMatch::new)
                .peek(dIdContainer -> dIdContainer.computeScore(1))
                .collect(Collectors.toSet());
    }

}
