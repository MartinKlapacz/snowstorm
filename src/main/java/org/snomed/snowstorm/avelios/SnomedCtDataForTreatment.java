package org.snomed.snowstorm.avelios;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collection;


@Getter
@Setter
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
    private Collection<String> directSctIdHits;

    @Field(type = FieldType.Keyword)
    private Collection<String> sctIdHitAncestors;

    public SnomedCtDataForTreatment(String patientId, String treatmentId, String visitId, Collection<String> directSctIdHits, Collection<String> sctIdHitAncestors) {
        this.patientId = patientId;
        this.treatmentId = treatmentId;
        this.visitId = visitId;
        this.directSctIdHits = directSctIdHits;
        this.sctIdHitAncestors = sctIdHitAncestors;
    }

}
