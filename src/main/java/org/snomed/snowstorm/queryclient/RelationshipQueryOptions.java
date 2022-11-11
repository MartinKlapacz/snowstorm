package org.snomed.snowstorm.queryclient;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
public class RelationshipQueryOptions {
    private String sourceConcept;
    private String destinationConcept;
    private Integer offset;
    private Integer limit;
}
