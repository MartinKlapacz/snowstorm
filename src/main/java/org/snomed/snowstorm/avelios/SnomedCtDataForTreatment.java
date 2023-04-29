package org.snomed.snowstorm.avelios;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;


public class SnomedCtDataForTreatment {

    @Field(type = FieldType.Keyword)
    @NotNull
    @Size(min = 36, max = 36)
    private String patientId;

    @Field(type = FieldType.Keyword)
    @NotNull
    @Size(min = 36, max = 36)
    private String treatmentId;

    @Field(type = FieldType.Keyword)
    @NotNull
    @Size(min = 36, max = 36)
    private String visitId;

    @Field(type = FieldType.Keyword)
    private List<String> directSctIdHits;

    @Field(type = FieldType.Keyword)
    private List<String> sctIdHitAncestors;

    public SnomedCtDataForTreatment(String patientId, String treatmentId, String visitId, List<String> directSctIdHits, List<String> sctIdHitAncestors) {
        this.patientId = patientId;
        this.treatmentId = treatmentId;
        this.visitId = visitId;
        this.directSctIdHits = directSctIdHits;
        this.sctIdHitAncestors = sctIdHitAncestors;
    }
}
