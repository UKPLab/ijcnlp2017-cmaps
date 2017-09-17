package grouping.clf.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Concept;
import preprocessing.NonUIMAPreprocessor;
import semilar.config.ConfigManager;
import semilar.sentencemetrics.AbstractComparer;
import semilar.sentencemetrics.OptimumComparer;
import semilar.sentencemetrics.PairwiseComparer.NormalizeType;
import semilar.sentencemetrics.PairwiseComparer.WordWeightType;
import semilar.tools.semantic.WordNetSimilarity;
import semilar.tools.semantic.WordNetSimilarity.WNSimMeasure;
import semilar.wordmetrics.LSAWordMetric;
import semilar.wordmetrics.WNWordMetric;

public class WordBasedMeasure extends ConceptSimilarityMeasure {

	private AbstractComparer comp;
	private String name;

	public WordBasedMeasure(WordNetSimilarity.WNSimMeasure wordSim) {
		WNWordMetric wordMetric = new WNWordMetric(wordSim, false);
		this.name = "WN-based " + wordSim;
		this.comp = new OptimumComparer(wordMetric, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);
	}

	public WordBasedMeasure(LSAWordMetric lsa) {
		this.name = "word-based LSA";
		this.comp = new OptimumComparer(lsa, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public double computeSimilarity(Concept c1, Concept c2) {
		if (c1.name.toLowerCase().equals(c2.name.toLowerCase()))
			return 1;

		NonUIMAPreprocessor prep = NonUIMAPreprocessor.getInstance();
		double sim = this.comp.computeSimilarity(prep.getSemilarSentence(c1), prep.getSemilarSentence(c2));

		return sim;
	}

	public static List<WordBasedMeasure> getAll() {
		List<WordBasedMeasure> all = new ArrayList<WordBasedMeasure>();

		// too slow: HSO, LESK, LESK_TANIM, LESK_TANIM_NOHYP
		WNSimMeasure[] exclude = { WNSimMeasure.HSO, WNSimMeasure.LESK, WNSimMeasure.LESK_TANIM,
				WNSimMeasure.LESK_TANIM_NOHYP };
		Set<WNSimMeasure> excludeSet = new HashSet<WNSimMeasure>(Arrays.asList(exclude));

		for (WNSimMeasure sim : WordNetSimilarity.WNSimMeasure.values())
			if (!excludeSet.contains(sim))
				all.add(new WordBasedMeasure(sim));

		ConfigManager.setSemilarDataRootFolder("../semilar/resources/");
		all.add(new WordBasedMeasure(new LSAWordMetric("LSA-MODEL-TASA-LEMMATIZED-DIM300")));

		return all;
	}

}
