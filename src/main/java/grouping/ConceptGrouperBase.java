package grouping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Concept;
import model.ConceptGroup;
import model.Proposition;
import pipeline.CmmComponent;
import pipeline.Extractor;

public class ConceptGrouperBase extends CmmComponent implements Extractor {

	protected List<Concept> concepts;
	protected List<Proposition> propositions;
	protected Set<List<Concept>> clusters;

	@Override
	public void processCollection() {

		// get extracted concepts and propositions
		Extractor ex = this.parent.getPrevExtractor(this);
		this.concepts = ex.getConcepts();
		this.propositions = ex.getPropositions();

		// group by same label
		Map<Concept, ConceptGroup> groups = LemmaGrouper.group(this.concepts);

		// create final cluster and update relations
		this.updateDataStructures(groups);

		this.parent.log(this, "grouped concepts: " + concepts.size());
		this.parent.log(this, "relations: " + propositions.size());
	}

	private void updateDataStructures(Map<Concept, ConceptGroup> groups) {

		// update concepts
		this.concepts.clear();
		this.concepts.addAll(groups.keySet());

		// build mapping
		Map<Concept, Concept> conceptMapping = new HashMap<Concept, Concept>();
		for (ConceptGroup group : groups.values()) {
			for (Concept c : group.getAll()) {
				conceptMapping.put(c, group.getRep());
			}
		}

		// adapt propositions
		List<Proposition> updated = new ArrayList<Proposition>();
		for (Proposition p : this.propositions) {
			p.sourceConcept = conceptMapping.get(p.sourceConcept);
			p.targetConcept = conceptMapping.get(p.targetConcept);
			if (p.sourceConcept != p.targetConcept)
				updated.add(p);
		}
		this.propositions = updated;

		// set clusters
		this.clusters = new HashSet<List<Concept>>();
		for (ConceptGroup group : groups.values())
			this.clusters.add(group.getAll());

	}

	@Override
	public List<Concept> getConcepts() {
		return this.concepts;
	}

	@Override
	public List<Proposition> getPropositions() {
		return this.propositions;
	}

	public Set<List<Concept>> getClusters() {
		return this.clusters;
	}

}
