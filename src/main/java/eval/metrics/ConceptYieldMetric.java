package eval.metrics;

import eval.Result;
import eval.matcher.Match;
import model.ConceptMap;

/**
 * Compares numbers of gold and extracted concepts
 * 
 * - ratio is stored as f1, though not really an f1 measure
 * 
 * @author falke
 *
 */
public class ConceptYieldMetric extends Metric {

	public ConceptYieldMetric(Match match) {
		super(match);
		this.name = "Concept Yield";
	}

	@Override
	public Result compare(ConceptMap evalMap, ConceptMap goldMap) {

		Result res = this.createResult(evalMap, goldMap);

		double eSize = evalMap.getConcepts().size();
		double gSize = goldMap.getConcepts().size();
		res.fMeasure = eSize / gSize;

		return res;
	}

}
