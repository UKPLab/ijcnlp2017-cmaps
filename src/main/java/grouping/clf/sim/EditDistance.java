package grouping.clf.sim;

import edu.stanford.nlp.util.StringUtils;
import model.Concept;

public class EditDistance extends ConceptSimilarityMeasure {

	@Override
	public double computeSimilarity(Concept c1, Concept c2) {
		if (c1.name.toLowerCase().equals(c2.name.toLowerCase()))
			return 1;
		double dist = StringUtils.editDistance(c1.name.toLowerCase().trim(), c2.name.toLowerCase().trim());
		int len = Math.max(c1.name.trim().length(), c2.name.trim().length());
		return Math.max(0, 1 - dist / len);
	}

}
