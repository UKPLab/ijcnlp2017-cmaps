package scoring.concepts.features;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;
import grouping.clf.sim.ConceptSimilarityMeasure;
import grouping.clf.sim.JaccardDistance;
import grouping.clf.sim.WordBasedMeasure;
import grouping.clf.sim.WordEmbeddingDistance;
import grouping.clf.sim.WordEmbeddingDistance.EmbeddingType;
import model.Concept;
import model.PToken;
import preprocessing.NonUIMAPreprocessor;
import semilar.tools.semantic.WordNetSimilarity.WNSimMeasure;
import util.FrequencyProvider;
import util.FrequencyProvider.Source;
import util.Muter;
import util.Stopwords;
import util.features.Feature;
import util.features.FeatureContainer;
import weka.core.Instances;

/**
 * Extraction of features for concept importance learning
 * 
 * @author falke
 *
 */
public class FeatureExtractor implements Serializable {

	private static final long serialVersionUID = 1L;
	private List<String> featureNames;
	private transient FeatureContainer<Concept> featContainer;
	private boolean isTraining;

	private String baseFolder;
	private String graphFile;
	private String topicFile;
	private Stopwords stopwords;
	private HashMap<Integer, String[]> topicDescriptions;
	private transient MRCFeatures mrc;
	private transient ConcFeatures conc;
	private transient LIWCFeatures liwc;
	private transient FrequencyProvider web1tFreq;

	public FeatureExtractor() {
		this.featureNames = new ArrayList<String>();
		this.isTraining = true;
		this.stopwords = new Stopwords("lists/stopwords_en_semilar.txt");
	}

	public void init(String baseFolder, String graphFile) {
		this.featContainer = new FeatureContainer<Concept>();
		this.baseFolder = baseFolder;
		this.graphFile = graphFile;
		this.topicFile = new File(baseFolder).getParent() + "/topics.tsv";
		this.loadTopicDesc();
		this.mrc = new MRCFeatures();
		this.conc = new ConcFeatures();
		this.liwc = new LIWCFeatures();
		this.web1tFreq = new FrequencyProvider(Source.WEB1T);
	}

	public void collectFeatures(Set<List<Concept>> clusters, int topic) {

		TopicFreqStatistics stats = this.computeFreqStats(clusters);
		Map<Integer, Map<String, Double>> graphFeatures = this.loadGraphFeatures(topic);

		for (List<Concept> cluster : clusters)
			this.computeFeatures(cluster, topic, stats, graphFeatures);

		if (this.isTraining) {
			this.featureNames = this.featContainer.getFeatures();
		}
	}

	public <T> void addLabels(Map<Concept, T> labels, String name) {
		for (Concept c : labels.keySet()) {
			this.featContainer.add(c, new Feature<T>("~_" + name, labels.get(c)));
		}
		if (this.isTraining) {
			this.featureNames = this.featContainer.getFeatures();
		}
	}

	public Instances getFeatures() {
		Instances data = this.featContainer.createInstances(this.featureNames);
		return data;
	}

	public void startTest() {
		this.isTraining = false;
		this.featContainer = new FeatureContainer<Concept>();
	}

	private void computeFeatures(List<Concept> cluster, int topic, TopicFreqStatistics stats,
			Map<Integer, Map<String, Double>> graphFeatures) {

		// representative concept
		Concept c = cluster.get(0);

		// meta
		featContainer.add(c, new Feature<Integer>("_id", c.id));
		featContainer.add(c, new Feature<Integer>("_topic", topic));

		// position
		featContainer.add(c, new Feature<Double>("position_rep", this.getPosition(c)));
		List<Double> positions = this.getPositions(cluster);
		featContainer.add(c, new Feature<Double>("position_first", positions.get(0)));
		featContainer.add(c, new Feature<Double>("position_last", positions.get(positions.size() - 1)));
		featContainer.add(c, new Feature<Double>("position_avg",
				positions.stream().mapToDouble(val -> val).average().getAsDouble()));
		featContainer.add(c,
				new Feature<Double>("position_spread", positions.get(0) - positions.get(positions.size() - 1)));

		// label - length
		featContainer.add(c, new Feature<Integer>("len_tok_rep", c.tokenList.size()));
		List<Integer> lens = this.getLength(cluster, false);
		featContainer.add(c, new Feature<Integer>("len_tok_max", lens.get(0)));
		featContainer.add(c, new Feature<Integer>("len_tok_min", lens.get(lens.size() - 1)));
		featContainer.add(c, new Feature<Double>("len_tok_avg",
				lens.stream().mapToDouble(Integer::doubleValue).average().getAsDouble()));
		featContainer.add(c, new Feature<Integer>("len_tok_spread", lens.get(0) - lens.get(lens.size() - 1)));

		featContainer.add(c, new Feature<Integer>("len_char_rep", c.name.length()));
		lens = this.getLength(cluster, true);
		featContainer.add(c, new Feature<Integer>("len_char_max", lens.get(0)));
		featContainer.add(c, new Feature<Integer>("len_char_min", lens.get(lens.size() - 1)));
		featContainer.add(c, new Feature<Double>("len_char_avg",
				lens.stream().mapToDouble(Integer::doubleValue).average().getAsDouble()));
		featContainer.add(c, new Feature<Integer>("len_char_spread", lens.get(0) - lens.get(lens.size() - 1)));

		// label - stopwords
		int nbSW = (int) c.tokenList.stream().filter(t -> this.stopwords.isSW(t.text.toLowerCase())).count();
		featContainer.add(c, new Feature<Integer>("sw_abs", nbSW));
		featContainer.add(c, new Feature<Double>("sw_rel", nbSW / (double) c.tokenList.size()));

		// label - capitalization
		String label = c.tokenList.stream().map(t -> t.text).collect(Collectors.joining(" "));
		featContainer.add(c, new Feature<Boolean>("cap_all", allUpperCase.matcher(label).matches()));
		featContainer.add(c, new Feature<Boolean>("cap_some", someUpperCase.matcher(label).matches()));

		// label - pos
		for (PToken t : c.tokenList)
			featContainer.add(c, new Feature<String>("pos", t.pos));

		// label - named entity
		for (PToken t : c.tokenList)
			featContainer.add(c, new Feature<String>("ne", t.neTag != null ? t.neTag : "O"));

		// label - head token
		if (c.headToken != null) {
			featContainer.add(c, new Feature<String>("head_pos", c.headToken.pos));
			featContainer.add(c, new Feature<String>("head_ne", c.headToken.neTag != null ? c.headToken.neTag : "O"));
			featContainer.add(c, new Feature<Integer>("head_dep_depth", c.headDepDepth));
		}

		// topic similarity
		for (Pair<String, Double> topicSim : this.computeTopicSimilarity(c, topic)) {
			featContainer.add(c, new Feature<Double>(topicSim.first(), topicSim.second()));
		}

		// psychologic categories
		for (Entry<String, Double> e : this.mrc.getFeatures(c).entrySet())
			featContainer.add(c, new Feature<Double>(e.getKey(), e.getValue()));
		for (Entry<String, Double> e : this.conc.getFeatures(c).entrySet())
			featContainer.add(c, new Feature<Double>(e.getKey(), e.getValue()));
		for (Entry<String, Double> e : this.liwc.getFeatures(c).entrySet())
			featContainer.add(c, new Feature<Double>(e.getKey(), e.getValue()));

		// open ie specific features
		featContainer.add(c, new Feature<Double>("ex_conf", c.confidence));
		if (!c.type.equals("SpatialArgument") && !c.type.equals("TemporalArgument"))
			c.type = "SimpleArgument";
		featContainer.add(c, new Feature<String>("ex_argtype", c.type));

		// frequency
		featContainer.add(c, new Feature<Integer>("freq_abs", cluster.size()));
		double freq_rel = cluster.size() / (double) stats.nbChars;
		featContainer.add(c, new Feature<Double>("freq_rel", freq_rel));

		Set<String> docs = cluster.stream().map(ci -> ci.tokenList.get(0).documentId).collect(Collectors.toSet());
		featContainer.add(c, new Feature<Double>("freq_docs", docs.size() / (double) stats.nbDoc));

		// idf
		List<Double> web1tIDF = this.getBackgroundFreq(c, x -> this.web1tFreq.getLogIDF(x));
		featContainer.add(c,
				new Feature<Double>("freq_idf_max", !web1tIDF.isEmpty() ? Collections.max(web1tIDF) : Double.NaN));
		featContainer.add(c,
				new Feature<Double>("freq_idf_min", !web1tIDF.isEmpty() ? Collections.min(web1tIDF) : Double.NaN));
		double avg = web1tIDF.stream().mapToDouble(x -> x).sum() / web1tIDF.size();
		featContainer.add(c, new Feature<Double>("freq_idf_avg", avg));

		featContainer.add(c, new Feature<Double>("freq_rel_idf_max",
				!web1tIDF.isEmpty() ? Collections.max(web1tIDF) * freq_rel : Double.NaN));
		featContainer.add(c, new Feature<Double>("freq_rel_idf_min",
				!web1tIDF.isEmpty() ? Collections.min(web1tIDF) * freq_rel : Double.NaN));
		featContainer.add(c, new Feature<Double>("freq_rel_idf_avg", avg * freq_rel));

		// graph-based features
		for (Entry<String, Double> gf : graphFeatures.get(c.id).entrySet())
			featContainer.add(c, new Feature<Double>("graph_" + gf.getKey(), gf.getValue()));

	}

	// collect idf values for every token
	private List<Double> getBackgroundFreq(Concept c, Function<String, Double> getFreq) {
		List<Double> idfs = new ArrayList<Double>();
		for (PToken t : c.tokenList) {
			double f = getFreq.apply(t.lemma.toLowerCase());
			if (!Double.isNaN(f))
				idfs.add(f);
		}
		return idfs;
	}

	// compute similarity with topic description
	private static Map<String, ConceptSimilarityMeasure> simMeasures;

	private List<Pair<String, Double>> computeTopicSimilarity(Concept c, int topic) {

		if (simMeasures == null) {
			simMeasures = new HashMap<String, ConceptSimilarityMeasure>();
			simMeasures.put("topic_jaccard", new JaccardDistance());
			simMeasures.put("topic_wn", new WordBasedMeasure(WNSimMeasure.RES));
			simMeasures.put("topic_w2v", new WordEmbeddingDistance(EmbeddingType.WORD2VEC, 300, false));
		}

		String[] topicDesc = this.topicDescriptions.get(topic);
		Concept dummy = new Concept(StringUtils.join(topicDesc));
		dummy = NonUIMAPreprocessor.getInstance().preprocess(dummy);

		List<Pair<String, Double>> scores = new ArrayList<Pair<String, Double>>();
		for (String sim : simMeasures.keySet()) {
			double score = Muter.callMuted(simMeasures.get(sim)::computeSimilarity, c, dummy);
			scores.add(new Pair<String, Double>(sim, score));
		}
		return scores;
	}

	// regex patterns
	private static final Pattern allUpperCase = Pattern.compile("[^a-z]*[A-Z]+[^a-z]*");
	private static final Pattern someUpperCase = Pattern.compile(".*[A-Z]+.*");

	// returns length of labels, in decreasing order
	private List<Integer> getLength(List<Concept> concepts, boolean chars) {
		List<Integer> lens = new ArrayList<Integer>();
		for (Concept c : concepts) {
			if (chars)
				lens.add(c.name.length());
			else
				lens.add(c.tokenList.size());
		}
		Collections.sort(lens, Collections.reverseOrder());
		return lens;
	}

	// returns all positions of this mention cluster, in decreasing order
	private List<Double> getPositions(List<Concept> concepts) {
		List<Double> pos = new ArrayList<Double>();
		for (Concept c : concepts)
			pos.add(this.getPosition(c));
		Collections.sort(pos, Collections.reverseOrder());
		return pos;
	}

	// returns position of concept mention (1 - beginning, 0 - end)
	private double getPosition(Concept c) {
		PToken firstToken = c.tokenList.get(0);
		return (firstToken.docLength - firstToken.start) / (double) firstToken.docLength;
	}

	// load precomputed graph-based features
	private Map<Integer, Map<String, Double>> loadGraphFeatures(int topic) {
		// concept -> feature_name -> value
		Map<Integer, Map<String, Double>> data = new HashMap<Integer, Map<String, Double>>();
		String fileName = this.baseFolder + "/" + topic + "/" + this.graphFile;
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), Charsets.UTF_8);
			String[] header = lines.get(0).split("\t");
			for (String line : lines.subList(1, lines.size())) {
				String[] cols = line.split("\t");
				int id = Integer.parseInt(cols[0]);
				Map<String, Double> features = new HashMap<String, Double>();
				for (int i = 1; i < cols.length; i++)
					features.put(header[i], Double.parseDouble(cols[i]));
				data.put(id, features);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	// load descriptions of all topics
	private void loadTopicDesc() {
		try {
			List<String> lines = FileUtils.readLines(new File(topicFile), Charsets.UTF_8);
			this.topicDescriptions = new HashMap<Integer, String[]>();
			for (String line : lines) {
				String[] cols = line.split("\t");
				String[] token = cols[1].split("\\s+");
				this.topicDescriptions.put(Integer.parseInt(cols[0]), token);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// compute statistics over the whole topic needed for frequencies
	private TopicFreqStatistics computeFreqStats(Set<List<Concept>> clusters) {
		TopicFreqStatistics stats = new TopicFreqStatistics();
		List<Concept> allConcepts = clusters.stream().flatMap(c -> c.stream()).collect(Collectors.toList());
		// stats.nbConcepts = allConcepts.size();
		List<PToken> allTokens = allConcepts.stream().map(c -> c.tokenList.get(0)).collect(Collectors.toList());
		stats.nbDoc = allTokens.stream().map(t -> t.documentId).collect(Collectors.toSet()).size();
		stats.nbChars = allTokens.stream().filter(distinctByKey(t -> t.documentId)).mapToInt(t -> t.docLength).sum();
		return stats;
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> seen = new HashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	private class TopicFreqStatistics {
		public int nbDoc;
		public int nbChars;
		// public int nbConcepts;
	}

}
