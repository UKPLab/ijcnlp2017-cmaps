package grouping.clf.sim;

import model.Concept;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

public class StemMatch extends ConceptSimilarityMeasure {

	private Stemmer stemmer;

	public StemMatch() {
		super();
		this.stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
	}

	@Override
	public double computeSimilarity(Concept c1, Concept c2) {
		if (this.stem(c1).equals(this.stem(c2))) {
			return 1.0;
		} else {
			return 0.0;
		}
	}

	protected String stem(Concept c) {
		StringBuffer stem = new StringBuffer();
		Tokenizer tok = SimpleTokenizer.INSTANCE;
		for (String word : tok.tokenize(c.name.toLowerCase().trim())) {
			stem.append(this.stemmer.stem(word));
			stem.append(" ");
		}
		return stem.toString().trim();
	}

}
