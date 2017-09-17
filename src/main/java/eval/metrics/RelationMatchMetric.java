package eval.metrics;

import eval.Result;
import eval.matcher.Match;
import model.ConceptMap;
import model.Proposition;
import util.CountedSet;

/**
 * Compares relation phrases of two concept maps, independent of concepts
 */
public class RelationMatchMetric extends Metric {

	public RelationMatchMetric(Match match) {
		super(match);
		this.name = "Relation";
	}

	@Override
	public Result compare(ConceptMap evalMap, ConceptMap goldMap) {

		CountedSet<String> evalRels = this.getRelationStringSet(evalMap);
		evalRels.setAllCounts(1);
		CountedSet<String> goldRels = this.getRelationStringSet(goldMap);
		goldRels.setAllCounts(1);

		Result res = this.createResult(evalMap, goldMap);
		return this.compareSets(res, evalRels, goldRels);
	}

	protected CountedSet<String> getRelationStringSet(ConceptMap map) {
		CountedSet<String> set = new CountedSet<String>();
		for (Proposition p : map.getProps()) {
			set.add(p.relationPhrase.toLowerCase());
		}
		return set;
	}
}
