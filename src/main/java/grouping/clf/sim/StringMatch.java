package grouping.clf.sim;

import model.Concept;

public class StringMatch extends ConceptSimilarityMeasure {

	@Override
	public double computeSimilarity(Concept c1, Concept c2) {
		if (c1.name.toLowerCase().trim().equals(c2.name.toLowerCase().trim())) {
			return 1.0;
		} else {
			return 0.0;
		}
	}

}
