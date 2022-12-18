package org.snomed.snowstorm.snomedConverter.queryclient;

import lombok.*;
import org.springframework.data.domain.PageRequest;

@Getter
@Setter
@ToString
@Builder
public class RelationshipQueryOptions {
    private String sourceConcept;
    private String destinationConcept;
    private Integer offset;
    private Integer limit;
    private String relationshipId;
    private PageRequest pageRequest;
}
