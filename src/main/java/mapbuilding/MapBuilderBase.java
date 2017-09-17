package mapbuilding;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Concept;
import model.ConceptMap;
import model.ExtractionResult;
import model.Proposition;
import pipeline.CmmComponent;
import pipeline.Extractor;

public abstract class MapBuilderBase extends CmmComponent {

	protected List<Concept> concepts;
	protected List<Proposition> propositions;
	protected ConceptMap map;

	@Override
	public void processCollection() {

		// get concepts and relations
		Extractor exComp = this.parent.getPrevExtractor(this);
		this.concepts = exComp.getConcepts();
		this.propositions = exComp.getPropositions();
		this.ensureUniqueConcepts();

		// create map
		this.map = new ConceptMap(this.parent.getName());
		this.buildMap();
		RelationSelector relSelector = new RelationSelector(p -> p.confidence);
		this.map = relSelector.select(this.map);

		int[] size = this.map.size();
		this.parent.log(this, "concepts: " + size[0] + ", relations: " + size[1]);

		// check score
		double totalScore = 0;
		for (Concept c : this.map.getConcepts())
			totalScore += c.weight;
		this.parent.log(this, "subgraph score " + totalScore);
	}

	abstract public void buildMap();

	@Override
	public ConceptMap getConceptMap() {
		return this.map;
	}

	protected void ensureUniqueConcepts() {
		Map<String, Set<Concept>> labelMapping = new HashMap<String, Set<Concept>>();
		for (Concept c : this.concepts) {
			labelMapping.putIfAbsent(c.name, new HashSet<Concept>());
			labelMapping.get(c.name).add(c);
		}
		Map<Concept, Concept> conceptReplace = new HashMap<Concept, Concept>();
		for (Set<Concept> group : labelMapping.values()) {
			if (group.size() > 1) {
				Concept rep = Collections.max(group);
				for (Concept c : group) {
					if (c != rep)
						conceptReplace.put(c, rep);
				}
			}
		}
		if (conceptReplace.size() > 0) {
			this.parent.log(this, "removing duplicate concepts: " + conceptReplace.size());
			this.concepts.removeAll(conceptReplace.keySet());
			for (Proposition p : this.propositions) {
				if (conceptReplace.containsKey(p.sourceConcept))
					p.sourceConcept = conceptReplace.get(p.sourceConcept);
				if (conceptReplace.containsKey(p.targetConcept))
					p.targetConcept = conceptReplace.get(p.targetConcept);
			}
		}
	}

	protected void buildMapFromSubset(Set<Concept> subset) {
		this.map = new ConceptMap(this.parent.getName());
		for (Concept c : this.concepts)
			if (subset.contains(c))
				this.map.addConcept(c);
		for (Proposition p : this.propositions)
			if (subset.contains(p.sourceConcept) && subset.contains(p.targetConcept))
				this.map.addProposition(p);
	}

	public void setData(ExtractionResult res) {
		this.concepts = res.concepts;
		this.propositions = res.propositions;
		this.map = new ConceptMap(this.parent.getName());
	}
}