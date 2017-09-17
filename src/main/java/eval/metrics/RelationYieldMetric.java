package eval.metrics;

import eval.Result;
import eval.matcher.Match;
import model.ConceptMap;

/**
 * Compares numbers of gold and extracted relations
 * 
 * - ratio is stored as f1, though not really an f1 measure
 * - can only be macro-averaged
 *
 */
public class RelationYieldMetric extends Metric {

	public RelationYieldMetric(Match match) {
		super(match);
		this.name = "Relation Yield";
	}

	@Override
	public Result compare(ConceptMap evalMap, ConceptMap goldMap) {

		Result res = this.createResult(evalMap, goldMap);

		double eSize = evalMap.getProps().size();
		double gSize = goldMap.getProps().size();
		res.fMeasure = eSize / gSize;

		return res;
	}

}
