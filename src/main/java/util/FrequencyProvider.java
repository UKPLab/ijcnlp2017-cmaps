package util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.jweb1t.JWeb1TSearcher;

public class FrequencyProvider {

	private static final File web1tPath = new File("C:\\Data\\web1t");

	private static final long web1tMax = 19401194714L;

	private Source source;
	private Map<String, Long> freqs;
	private JWeb1TSearcher freqSearcher;

	public FrequencyProvider(Source source) {

		this.source = source;
		this.freqs = new HashMap<String, Long>(100000);

		if (this.source == Source.WEB1T) {
			System.out.println("loading Google Web1T ngrams");
			try {
				freqSearcher = new JWeb1TSearcher(web1tPath, 1, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("loaded");
		}
	}

	public long getDocCount() {
		return this.web1tMax;
	}

	public double getDF(String phrase) {

		Long df = freqs.get(phrase);

		if (df == null && this.source == Source.WEB1T) {
			try {
				df = freqSearcher.getFrequency(phrase);
			} catch (IOException e) {
				System.err.println("web1t error: " + phrase);
			}
			freqs.put(phrase, df);
		}

		if (df != null)
			return df / (double) this.getDocCount();
		else
			return 0;
	}

	public double getIDF(String phrase) {
		double df = this.getDF(phrase);
		if (df == 0)
			return Double.NaN;
		else
			return 1 / df;
	}

	public double getLogIDF(String phrase) {
		double idf = this.getIDF(phrase);
		if (Double.isNaN(idf))
			return Double.NaN;
		else
			return Math.log10(idf);
	}

	public enum Source {
		WEB1T
	}

}
