package grouping.clf.sim;

import model.Concept;

public abstract class ConceptSimilarityMeasure {

	abstract public double computeSimilarity(Concept c1, Concept c2);

	public String getName() {
		return this.getClass().getSimpleName();
	}

}
