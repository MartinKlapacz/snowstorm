package org.snomed.snowstorm.rest.pojo;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.rest.View;

import java.util.Set;

@NoArgsConstructor
@Getter
public class ConceptDescriptionsResult {

	private Set<Description> conceptDescriptions;

	public ConceptDescriptionsResult(Set<Description> conceptDescriptions) {
		this.conceptDescriptions = conceptDescriptions;
	}

	@JsonView(value = View.Component.class)
	public Set<Description> getConceptDescriptions() {
		return conceptDescriptions;
	}
}
