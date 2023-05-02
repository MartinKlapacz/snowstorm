package org.snomed.snowstorm.avelios;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collection;


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
    // todo fix lombok
    public String getPatientId() {
        return patientId;
    }

    public String getTreatmentId() {
        return treatmentId;
    }

    public String getVisitId() {
        return visitId;
    }

    public Collection<String> getDirectSctIdHits() {
        return directSctIdHits;
    }

    public Collection<String> getSctIdHitAncestors() {
        return sctIdHitAncestors;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setTreatmentId(String treatmentId) {
        this.treatmentId = treatmentId;
    }

    public void setVisitId(String visitId) {
        this.visitId = visitId;
    }

    public void setDirectSctIdHits(Collection<String> directSctIdHits) {
        this.directSctIdHits = directSctIdHits;
    }

    public void setSctIdHitAncestors(Collection<String> sctIdHitAncestors) {
        this.sctIdHitAncestors = sctIdHitAncestors;
    }
}
