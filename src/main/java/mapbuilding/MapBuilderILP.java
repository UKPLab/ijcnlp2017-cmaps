package mapbuilding;

import java.util.Set;

import mapbuilding.ilp.GraphSummarizer;
import mapbuilding.ilp.SubgraphILPByComponent;
import model.Concept;

public class MapBuilderILP extends MapBuilderBase {

	public static int ilpTimeout = 300;

	@Override
	public void buildMap() {

		// set up and solve ILP
		GraphSummarizer ilp = new SubgraphILPByComponent(this.concepts, this.propositions, this.parent.getMaxConcepts(),
				ilpTimeout);
		Set<Concept> subgraph = ilp.getSubgraph();

		this.buildMapFromSubset(subgraph);
	}

}