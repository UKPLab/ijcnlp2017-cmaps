package scoring.concepts.features;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import model.Concept;
import model.PToken;

public class ConcFeatures {

	private static final String FILE = "dict/Brysbaert.tsv";

	// map: word -> score
	// note: words can also be bigrams (7%)
	private Map<String, Double> data;

	public ConcFeatures() {
		this.data = new HashMap<String, Double>();
		this.loadData();
	}

	public Map<String, Double> getFeatures(Concept c) {
		Map<String, Double> feat = new HashMap<String, Double>();
		Collection<Double> values = new ArrayList<Double>();
		// try bigrams
		Set<PToken> inBigram = new HashSet<PToken>();
		for (int i = 0; i < c.tokenList.size() - 1; i++) {
			String bigram = c.tokenList.get(i).lemma.toLowerCase() + " " + c.tokenList.get(i + 1).lemma.toLowerCase();
			Double v = this.data.get(bigram);
			if (v != null) {
				values.add(v);
				inBigram.addAll(c.tokenList.subList(i, i + 2));
			}
		}
		// try unigrams
		for (PToken t : c.tokenList) {
			if (!inBigram.contains(t)) {
				String unigram = t.lemma.toLowerCase();
				Double v = this.data.get(unigram);
				if (v != null)
					values.add(v);
			}
		}
		feat.put("conc_max", !values.isEmpty() ? Collections.max(values) : Double.NaN);
		feat.put("conc_min", !values.isEmpty() ? Collections.min(values) : Double.NaN);
		double avg = values.stream().mapToDouble(x -> x).sum() / values.size();
		feat.put("conc_avg", avg);
		return feat;
	}

	private void loadData() {
		File f = new File(getClass().getClassLoader().getResource(FILE).getFile());
		try {
			LineIterator i = FileUtils.lineIterator(f);
			while (i.hasNext()) {
				String[] cols = i.next().split("\t");
				if (!cols[0].equals("Word")) {
					String w = cols[0].toLowerCase();
					Double s = Double.parseDouble(cols[2]);
					this.data.put(w, s);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
