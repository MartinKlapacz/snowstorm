package org.snomed.snowstorm.avelios.converterPipeline;

import lombok.Getter;
import lombok.ToString;
import org.snomed.snowstorm.core.data.domain.Description;

import java.util.Comparator;

@Getter
@ToString
public class DescriptionMatch {
    public static final Comparator<DescriptionMatch> COMPARATOR = (o1, o2) -> {
        int res = Double.compare(o1.getScore(), o2.getScore());
        if (res == 0){
            return Integer.compare(o1.fullDescriptionTerm.length(), o2.fullDescriptionTerm.length());
        }
        return res;
    };
    private final String descriptionId;
    private int score;

    // for debugging
    private final String fullDescriptionTerm;

    private final float numOfTokensInDescriptionTerm;

    public DescriptionMatch(Description description) {
        this.descriptionId = description.getDescriptionId();
        this.fullDescriptionTerm = description.getTerm();
        this.numOfTokensInDescriptionTerm = description.getTerm().split(" ").length;
    }

    public DescriptionMatch computeScore(int numOfMatchingTokens) {
        score = (int) (100 * numOfMatchingTokens / numOfTokensInDescriptionTerm);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DescriptionMatch) {
            return descriptionId.equals(((DescriptionMatch) obj).descriptionId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return descriptionId.hashCode();
    }
}
