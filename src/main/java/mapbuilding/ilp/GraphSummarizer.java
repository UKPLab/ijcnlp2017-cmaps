package mapbuilding.ilp;

import java.util.List;
import java.util.Set;

import model.Concept;
import model.Proposition;

public abstract class GraphSummarizer {

	protected List<Concept> concepts;
	protected List<Proposition> propositions;
	protected int maxSize;

	public GraphSummarizer(List<Concept> concepts, List<Proposition> propositions, int maxSize) {
		this.concepts = concepts;
		this.propositions = propositions;
		this.maxSize = maxSize;
	}

	public abstract Set<Concept> getSubgraph();

}
