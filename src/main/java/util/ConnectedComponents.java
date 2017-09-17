package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import model.Concept;
import model.Proposition;

public class ConnectedComponents {

	public static List<Set<Concept>> findConnectedComponents(List<Concept> concepts, List<Proposition> propositions) {

		Map<Concept, Set<Proposition>> propByConcept = new HashMap<Concept, Set<Proposition>>();
		for (Proposition p : propositions) {
			propByConcept.putIfAbsent(p.sourceConcept, new HashSet<Proposition>());
			propByConcept.get(p.sourceConcept).add(p);
			propByConcept.putIfAbsent(p.targetConcept, new HashSet<Proposition>());
			propByConcept.get(p.targetConcept).add(p);
		}
		for (Concept c : concepts)
			propByConcept.putIfAbsent(c, new HashSet<Proposition>());

		return findConnectedComponents(concepts, propByConcept);
	}

	public static List<Set<Concept>> findConnectedComponents(List<Concept> concepts,
			Map<Concept, Set<Proposition>> propsByConcept) {

		List<Set<Concept>> components = new ArrayList<Set<Concept>>();
		Set<Concept> notVisited = new HashSet<Concept>(concepts);

		while (!notVisited.isEmpty()) {

			Concept first = notVisited.iterator().next();
			notVisited.remove(first);
			Queue<Concept> neighbourQueue = new LinkedList<Concept>();
			neighbourQueue.add(first);
			Set<Concept> component = new HashSet<Concept>();

			while (!neighbourQueue.isEmpty()) {
				Concept c = neighbourQueue.poll();
				component.add(c);
				for (Proposition p : propsByConcept.get(c)) {
					if (notVisited.contains(p.sourceConcept)) {
						neighbourQueue.add(p.sourceConcept);
						notVisited.remove(p.sourceConcept);
					}
					if (notVisited.contains(p.targetConcept)) {
						neighbourQueue.add(p.targetConcept);
						notVisited.remove(p.targetConcept);
					}
				}
			}
			components.add(component);
		}

		Collections.sort(components, new Comparator<Set<Concept>>() {
			@Override
			public int compare(Set<Concept> o1, Set<Concept> o2) {
				return o2.size() - o1.size();
			}
		});
		return components;
	}
}
