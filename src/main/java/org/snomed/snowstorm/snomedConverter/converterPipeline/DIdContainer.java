package org.snomed.snowstorm.snomedConverter.converterPipeline;

import lombok.Getter;
import lombok.ToString;
import org.snomed.snowstorm.core.data.domain.Description;

import java.util.Comparator;

@Getter
@ToString
public class DIdContainer {
    public static final Comparator<DIdContainer> COMPARATOR = (o1, o2) -> o1.descriptionId.compareTo(o2.getDescriptionId());
    private final String descriptionId;
    private final int descriptionLength;
    private double score;

    // for debugging
    private final String term;

    public DIdContainer(Description description) {
        this.descriptionId = description.getDescriptionId();
        this.descriptionLength = description.getTerm().length();
        this.term = description.getTerm();
    }

    public void computeScore(int tokenLength) {
        score =  tokenLength * 1.0 / descriptionLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DIdContainer) {
            return descriptionId.equals(((DIdContainer) obj).descriptionId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return descriptionId.hashCode();
    }
}
