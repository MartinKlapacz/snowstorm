package org.snomed.snowstorm.core.data.repositories;

import org.snomed.snowstorm.core.data.domain.Concept;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface ConceptRepository extends ElasticsearchRepository<Concept, String> {
    Optional<Concept> findConceptByConceptId(String conceptId);

}
