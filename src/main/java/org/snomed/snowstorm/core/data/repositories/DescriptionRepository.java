package org.snomed.snowstorm.core.data.repositories;

import org.snomed.snowstorm.core.data.domain.Description;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface DescriptionRepository extends ElasticsearchRepository<Description, String> {
    List<Description> findDescriptionByDescriptionId(String descriptionId);
}
