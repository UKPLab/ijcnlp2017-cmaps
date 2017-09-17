package grouping.clf.sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import edu.stanford.nlp.util.Sets;
import model.Concept;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

public class JaccardDistance extends ConceptSimilarityMeasure {

	private Stemmer stemmer;
	private Set<String> functionWords;

	public JaccardDistance() {
		super();
		this.stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
		this.functionWords = this
				.loadFunctionWords(getClass().getClassLoader().getResource("lists/functionwords_en.txt"));
	}

	@Override
	public double computeSimilarity(Concept c1, Concept c2) {
		if (c1.name.toLowerCase().equals(c2.name.toLowerCase()))
			return 1;

		Set<String> c1Stems = this.stem(c1);
		Set<String> c2Stems = this.stem(c2);

		Set<String> intsect = Sets.intersection(c1Stems, c2Stems);
		Set<String> union = Sets.union(c1Stems, c2Stems);

		double sim = 0;
		if (union.size() == 0)
			sim = 0;
		else
			sim = intsect.size() / (float) union.size();

		return sim;
	}

	protected Set<String> stem(Concept c) {
		Set<String> stems = new HashSet<String>();
		Tokenizer tok = SimpleTokenizer.INSTANCE;
		for (String word : tok.tokenize(c.name.toLowerCase().trim())) {
			if (!this.functionWords.contains(word))
				stems.add((String) this.stemmer.stem(word));
		}
		return stems;
	}

	private Set<String> loadFunctionWords(URL fileUrl) {
		Set<String> words = new HashSet<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileUrl.getPath()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				words.add(line.toLowerCase().trim());
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return words;
	}

}
