package scoring.concepts.features;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import model.Concept;
import model.PToken;

public class MRCFeatures {

	private static final String FILE = "dict/MRC.tsv";

	// map: dimension -> word -> score
	private Map<String, Map<String, Integer>> data;

	public MRCFeatures() {
		this.data = new HashMap<String, Map<String, Integer>>();
		this.loadData();
	}

	public Map<String, Double> getFeatures(Concept c) {
		Map<String, Double> feat = new HashMap<String, Double>();
		for (String f : this.data.keySet()) {
			Collection<Double> values = new ArrayList<Double>();
			for (PToken t : c.tokenList) {
				Integer v = this.data.get(f).get(t.text.toLowerCase().trim());
				if (v == null)
					v = this.data.get(f).get(t.lemma.toLowerCase().trim());
				if (v != null)
					values.add(v.doubleValue());
			}
			feat.put("MRC_" + f + "_max", !values.isEmpty() ? Collections.max(values) : Double.NaN);
			feat.put("MRC_" + f + "_min", !values.isEmpty() ? Collections.min(values) : Double.NaN);
			double avg = values.stream().mapToDouble(x -> x).sum() / values.size();
			feat.put("MRC_" + f + "_avg", avg);
		}
		return feat;
	}

	private void loadData() {
		File f = new File(getClass().getClassLoader().getResource(FILE).getFile());
		try {
			LineIterator i = FileUtils.lineIterator(f);
			String[] header = null;
			while (i.hasNext()) {
				String[] cols = i.next().split("\t");
				if (cols[0].equals("word")) {
					for (int c = 1; c < cols.length; c++)
						this.data.put(cols[c], new HashMap<String, Integer>());
					header = cols;
				} else {
					String w = cols[0].toLowerCase();
					for (int c = 1; c < cols.length; c++) {
						if (cols[c].length() > 0)
							this.data.get(header[c]).put(w, Integer.parseInt(cols[c]));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
