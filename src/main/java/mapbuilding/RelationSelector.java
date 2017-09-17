package mapbuilding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.Sets;

import model.Concept;
import model.ConceptMap;
import model.PToken;
import model.Proposition;

public class RelationSelector {

	private Function<Proposition, Double> keyFn;

	public RelationSelector(Function<Proposition, Double> keyFn) {
		this.keyFn = keyFn;
	}

	public ConceptMap select(ConceptMap fullMap) {

		Set<Proposition> selectedProps = new HashSet<Proposition>();

		int countSingle = 0;
		int countSelect = 0;

		// for all pairs of concepts
		List<Concept> concepts = new ArrayList<Concept>(fullMap.getConcepts());
		for (int i = 0; i < concepts.size(); i++) {
			for (int j = i + 1; j < concepts.size(); j++) {
				Concept c1 = concepts.get(i);
				Concept c2 = concepts.get(j);

				// get all potential relations
				Set<Proposition> pairProps = Sets.intersection(fullMap.getProps(c1, true), fullMap.getProps(c2, true));
				if (pairProps.size() == 0)
					continue;

				if (pairProps.size() == 1) {

					selectedProps.add(pairProps.iterator().next());
					countSingle++;

				} else {

					// group by label
					Map<String, Set<Proposition>> byName = new HashMap<String, Set<Proposition>>();
					for (Proposition p : pairProps) {
						String label = this.getLabel(p);
						byName.putIfAbsent(label, new HashSet<Proposition>());
						byName.get(label).add(p);
					}
					Collection<Set<Proposition>> grouped = byName.values();

					// select by confidence
					Proposition best = null;
					double bestScore = 0;
					for (Set<Proposition> group : grouped) {
						Proposition rep = Collections.max(group,
								(a, b) -> Double.compare(this.keyFn.apply(b), this.keyFn.apply(a)));
						rep.weight = group.size(); // to find most frequent
						if (this.keyFn.apply(rep) > bestScore) {
							bestScore = this.keyFn.apply(rep);
							best = rep;
						}
					}
					selectedProps.add(best);
					countSelect++;
				}
			}
		}
		// System.out.println("single: " + countSingle);
		// System.out.println("select: " + countSelect);

		// create new map
		ConceptMap map = new ConceptMap(fullMap.getName());
		map.addConcept(fullMap.getConcepts());
		map.addProposition(selectedProps);

		return map;
	}

	private String getLabel(Proposition p) {
		StringBuilder sb = new StringBuilder();
		for (PToken t : p.relationPhraseToken) {
			sb.append(t.lemma.toLowerCase());
			sb.append(" ");
		}
		return sb.toString();
	}

}
