package org.snomed.snowstorm.avelios.queryclient;

import lombok.Data;

@Data
public class TreatmentFinish {
    private String treatmentId;
    private String patientId;
    private String visitId;
}
