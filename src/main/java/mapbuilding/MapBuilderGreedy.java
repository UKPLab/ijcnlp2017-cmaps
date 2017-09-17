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

/**
 * Greedily add highest neighbor to build connected map
 * 
 * @author falke
 *
 */
public class MapBuilderGreedy extends MapBuilderBase {

	protected Map<Concept, Set<Proposition>> propByConcept;

	@Override
	public void buildMap() {

		// adjacency list
		this.propByConcept = new HashMap<Concept, Set<Proposition>>();
		for (Proposition p : this.propositions) {
			this.propByConcept.putIfAbsent(p.sourceConcept, new HashSet<Proposition>());
			this.propByConcept.get(p.sourceConcept).add(p);
			this.propByConcept.putIfAbsent(p.targetConcept, new HashSet<Proposition>());
			this.propByConcept.get(p.targetConcept).add(p);
		}
		for (Concept c : this.concepts)
			this.propByConcept.putIfAbsent(c, new HashSet<Proposition>());

		// greedy concept selection
		Set<Concept> subset = new HashSet<Concept>();
		subset.add(this.concepts.get(0));

		while (subset.size() < this.parent.getMaxConcepts()) {

			Set<Concept> neighbors = new HashSet<Concept>();
			for (Concept c : subset) {
				for (Proposition p : this.propByConcept.get(c)) {
					if (!subset.contains(p.sourceConcept))
						neighbors.add(p.sourceConcept);
					if (!subset.contains(p.targetConcept))
						neighbors.add(p.targetConcept);
				}
			}

			List<Concept> sorted = new ArrayList<Concept>(neighbors);
			Collections.sort(sorted);
			subset.add(sorted.get(0));
		}

		this.buildMapFromSubset(subset);
	}

}