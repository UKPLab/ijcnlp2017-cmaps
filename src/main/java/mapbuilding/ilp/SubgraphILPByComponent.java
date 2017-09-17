package mapbuilding.ilp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import model.Concept;
import model.Proposition;
import util.ConnectedComponents;

public class SubgraphILPByComponent extends GraphSummarizer {

	private int maxTime = -1;

	public SubgraphILPByComponent(List<Concept> concepts, List<Proposition> propositions, int maxSize) {
		super(concepts, propositions, maxSize);
	}

	public SubgraphILPByComponent(List<Concept> concepts, List<Proposition> propositions, int maxSize, int maxTime) {
		super(concepts, propositions, maxSize);
		this.maxTime = maxTime;
	}

	@Override
	public Set<Concept> getSubgraph() {

		// build adjacency list
		Map<Concept, Set<Proposition>> propsByConcept = new HashMap<Concept, Set<Proposition>>();
		for (Proposition p : propositions) {
			propsByConcept.putIfAbsent(p.sourceConcept, new HashSet<Proposition>());
			propsByConcept.get(p.sourceConcept).add(p);
			propsByConcept.putIfAbsent(p.targetConcept, new HashSet<Proposition>());
			propsByConcept.get(p.targetConcept).add(p);
		}
		for (Concept c : concepts)
			propsByConcept.putIfAbsent(c, new HashSet<Proposition>());

		// find connected components
		List<Set<Concept>> components = ConnectedComponents.findConnectedComponents(concepts, propsByConcept);

		// check each component
		double bestScore = -Double.MAX_VALUE;
		Set<Concept> bestSubgraph = null;
		for (Set<Concept> component : components) {

			// if even the full component cannot be better, skip
			double maxScore = component.stream().mapToDouble(x -> x.weight).sum();
			if (maxScore <= bestScore)
				continue;

			double score;
			Set<Concept> subgraph = null;

			// if smaller than max -> already a solution
			if (component.size() <= this.maxSize) {

				score = maxScore;
				subgraph = component;

			} else {

				// if not, run ILP to find best subgraph
				Set<Proposition> compProps = component.stream().flatMap(x -> propsByConcept.get(x).stream())
						.collect(Collectors.toSet());
				SubgraphILPFast ilp = new SubgraphILPFast(new ArrayList<Concept>(component),
						new ArrayList<Proposition>(compProps), maxSize, maxTime);
				subgraph = ilp.getSubgraph();
				score = subgraph.stream().mapToDouble(x -> x.weight).sum();
			}

			if (score > bestScore) {
				bestScore = score;
				bestSubgraph = subgraph;
			}
		}

		return bestSubgraph;
	}

}
