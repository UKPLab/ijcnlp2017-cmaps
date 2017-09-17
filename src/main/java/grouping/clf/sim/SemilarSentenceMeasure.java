package grouping.clf.sim;

import java.util.ArrayList;
import java.util.List;

import model.Concept;
import preprocessing.NonUIMAPreprocessor;
import semilar.config.ConfigManager;
import semilar.sentencemetrics.AbstractComparer;
import semilar.sentencemetrics.CorleyMihalceaComparer;
import semilar.sentencemetrics.LSAComparer;
import semilar.sentencemetrics.LexicalOverlapComparer;

public class SemilarSentenceMeasure extends ConceptSimilarityMeasure {

	private AbstractComparer comp;

	public SemilarSentenceMeasure(AbstractComparer comp) {
		this.comp = comp;
	}

	@Override
	public String getName() {
		return this.comp.getClass().getSimpleName();
	}

	@Override
	public double computeSimilarity(Concept c1, Concept c2) {

		NonUIMAPreprocessor prep = NonUIMAPreprocessor.getInstance();
		double sim = this.comp.computeSimilarity(prep.getSemilarSentence(c1), prep.getSemilarSentence(c2));

		if (sim < 0 || sim > 1 || Double.isNaN(sim) || sim == Double.POSITIVE_INFINITY)
			System.err.println(this.getName() + ": " + sim);

		return sim;
	}

	public static List<SemilarSentenceMeasure> getAll() {
		List<SemilarSentenceMeasure> all = new ArrayList<SemilarSentenceMeasure>();
		all.add(new SemilarSentenceMeasure(new LexicalOverlapComparer(true)));
		// BLEU gives always NaN
		// all.add(new SemilarSentenceMeasure(new BLEUComparer()));
		all.add(new SemilarSentenceMeasure(new CorleyMihalceaComparer(0.3f, false, "NONE", "par")));
		ConfigManager.setSemilarDataRootFolder("../semilar/resources/");
		all.add(new SemilarSentenceMeasure(new LSAComparer("LSA-MODEL-TASA-LEMMATIZED-DIM300")));
		return all;
	}

}
