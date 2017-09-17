package eval.matcher;

import java.util.Arrays;
import java.util.List;

/**
 * system label includes gold label after stemming of each token
 */
public class InclusiveMatch extends StemMatch {

	public InclusiveMatch() {
		super();
		this.name = "Inclusive Match";
	}

	@Override
	public boolean isMatch(String g, String e) {
		String[] stemmedGold = stem(clean(g)).split(" ");
		List<String> gold = Arrays.asList(stemmedGold);
		String[] stemmedEval = stem(clean(e)).split(" ");
		List<String> eval = Arrays.asList(stemmedEval);
		return eval.containsAll(gold);
	}

}
