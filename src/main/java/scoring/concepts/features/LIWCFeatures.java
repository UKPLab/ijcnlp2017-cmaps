package scoring.concepts.features;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import model.Concept;
import model.PToken;

public class LIWCFeatures {

	private static final String FILE_WORDS = "dict/LIWC.csv";
	private static final String FILE_CAT = "dict/LIWC.names";

	private Map<String, String[]> wordMap;
	private Map<String, String> catMap;
	private String[] cats;

	public LIWCFeatures() {
		this.wordMap = new HashMap<String, String[]>();
		this.catMap = new HashMap<String, String>();
		this.loadData();
		this.cats = new String[catMap.keySet().size()];
		int i = 0;
		for (String cat : catMap.keySet())
			cats[i++] = cat;
	}

	public Map<String, Double> getFeatures(Concept c) {
		Map<String, Double> feat = new HashMap<String, Double>();
		int[] counts = new int[cats.length];
		for (PToken t : c.tokenList) {
			Set<String> tokCats = this.getCategories(t.text.toLowerCase().trim() + "*");
			for (int i = 0; i < cats.length; i++)
				if (tokCats.contains(cats[i]))
					counts[i]++;
		}
		for (int i = 0; i < cats.length; i++) {
			feat.put("LIWC_" + this.catMap.get(cats[i]), counts[i] / (double) c.tokenList.size());
		}
		return feat;

	}

	private Set<String> getCategories(String word) {
		String[] cats = this.wordMap.get(word);
		if (cats != null)
			return new HashSet<String>(Arrays.asList(cats).subList(1, cats.length));
		else if (word.length() == 2)
			return new HashSet<String>();
		else
			return this.getCategories(word.substring(0, word.length() - 2) + '*');
	}

	private void loadData() {
		File f = new File(getClass().getClassLoader().getResource(FILE_WORDS).getFile());
		try {
			LineIterator i = FileUtils.lineIterator(f);
			while (i.hasNext()) {
				String[] cols = i.next().split(",");
				String w = cols[0].toLowerCase();
				if (w.charAt(w.length() - 1) != '*')
					w += '*';
				this.wordMap.put(w, cols);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		f = new File(getClass().getClassLoader().getResource(FILE_CAT).getFile());
		try {
			LineIterator i = FileUtils.lineIterator(f);
			while (i.hasNext()) {
				String[] cols = i.next().split("\t");
				this.catMap.put(cols[0], cols[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
