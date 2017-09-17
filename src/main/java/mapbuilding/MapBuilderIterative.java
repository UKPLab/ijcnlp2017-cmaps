package mapbuilding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Concept;
import model.Proposition;
import util.ConnectedComponents;

public class MapBuilderIterative extends MapBuilderBase {

	protected Map<Concept, Set<Proposition>> propByConcept;

	@Override
	public void buildMap() {

		this.propByConcept = new HashMap<Concept, Set<Proposition>>();
		for (Proposition p : this.propositions) {
			this.propByConcept.putIfAbsent(p.sourceConcept, new HashSet<Proposition>());
			this.propByConcept.get(p.sourceConcept).add(p);
			this.propByConcept.putIfAbsent(p.targetConcept, new HashSet<Proposition>());
			this.propByConcept.get(p.targetConcept).add(p);
		}
		for (Concept c : this.concepts)
			this.propByConcept.putIfAbsent(c, new HashSet<Proposition>());

		Set<Concept> subset = this.buildMapIteratively();
		this.buildMapFromSubset(subset);
	}

	// build map by removing lowest concepts and follow biggest connected parts
	protected Set<Concept> buildMapIteratively() {

		Set<Concept> concepts = new HashSet<Concept>(this.concepts);
		List<Set<Concept>> components = ConnectedComponents.findConnectedComponents(this.concepts, this.propByConcept);
		int biggest = components.get(0).size();
		while (concepts.size() > this.parent.getMaxConcepts()) {

			// remove too small components
			for (Set<Concept> component : components) {
				if (component.size() < this.parent.getMaxConcepts() && component.size() < biggest) {
					concepts.removeAll(component);
				}
			}

			// remove weakest concept
			Concept weakest = Collections.max(concepts); // natural = reverse
			concepts.remove(weakest);

			// find new components
			components = ConnectedComponents.findConnectedComponents(new ArrayList<Concept>(concepts),
					this.propByConcept);
			biggest = components.get(0).size();
		}

		return concepts;
	}

}