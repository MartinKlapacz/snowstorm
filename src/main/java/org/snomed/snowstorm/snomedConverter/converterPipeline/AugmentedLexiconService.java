package org.snomed.snowstorm.snomedConverter.converterPipeline;

import org.snomed.snowstorm.core.data.domain.ConceptMini;
import org.snomed.snowstorm.snomedConverter.queryclient.SnowstormEndpointsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AugmentedLexiconService {

    @Autowired
    SnowstormEndpointsClient snowstormEndpointsClient;

    public Set<DIdContainer> getConceptsContainingToken(String token) {
        Set<ConceptMini> descriptionResults = snowstormEndpointsClient.browserDescriptionSearch(token);

        return descriptionResults.stream()
                .map(ConceptMini::getActiveDescriptions)
                .flatMap(Collection::stream)
                .map(DIdContainer::new)
                .peek(dIdContainer -> dIdContainer.computeScore(token.length()))
                .collect(Collectors.toSet());
    }

}
