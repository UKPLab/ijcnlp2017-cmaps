package grouping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import com.carrotsearch.hppc.ObjectDoubleHashMap;
import com.carrotsearch.hppc.ObjectDoubleMap;
import com.carrotsearch.hppc.cursors.ObjectDoubleCursor;

import edu.stanford.nlp.util.Pair;
import grouping.clf.sim.CachedSim;
import grouping.clf.sim.ConceptSimilarityMeasure;
import grouping.clf.sim.EditDistance;
import grouping.clf.sim.JaccardDistance;
import grouping.clf.sim.WordBasedMeasure;
import grouping.clf.sim.WordEmbeddingDistance;
import grouping.clf.sim.WordEmbeddingDistance.EmbeddingType;
import grouping.clustering.AbstractConceptClusterer;
import grouping.clustering.GreedyLocalOptimizer;
import model.CPair;
import model.Concept;
import model.ConceptGroup;
import model.PToken;
import model.Proposition;
import pipeline.Extractor;
import preprocessing.NonUIMAPreprocessor;
import semilar.config.ConfigManager;
import semilar.tools.semantic.WordNetSimilarity;
import semilar.wordmetrics.LSAWordMetric;
import util.Muter;
import util.StringPair;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Concept grouping with similarity-based logistic classifier
 * 
 * @author falke
 *
 */
public class ConceptGrouperSimLog extends ConceptGrouperBase implements Extractor {

	private static final String cacheFolder = "data/grouping/cache";
	private static final int pairsPerFile = 10000000;
	private static boolean saveCache = false;
	private static boolean parallel = false;

	private final String modelName = "models/grouping_Logistic-sim5.model";
	private AbstractConceptClusterer clusterer = new GreedyLocalOptimizer();

	@Override
	public void processCollection() {

		String topic = this.parent.getTargetLocation().substring(this.parent.getTargetLocation().lastIndexOf("/") + 1);

		// get extracted concepts and propositions
		Extractor ex = this.parent.getPrevExtractor(this);
		this.concepts = ex.getConcepts();
		this.propositions = ex.getPropositions();
		for (Concept c : this.concepts)
			this.fixLemmas(c);

		// group by same label
		Map<Concept, ConceptGroup> groups = LemmaGrouper.group(this.concepts);
		List<Concept> repConcepts = new ArrayList<Concept>(groups.keySet());
		this.parent.log(this, "unique concepts: " + groups.size());

		// build all pairs for classifier
		List<CPair> pairs = this.buildPairs(repConcepts);
		this.parent.log(this, "concept pairs: " + pairs.size());

		// compute similarity features
		Instances features = this.computeFeatures(pairs, topic);

		// apply classifier
		ObjectDoubleMap<CPair> predictions = new ObjectDoubleHashMap<CPair>(pairs.size());
		try {
			Classifier clf = (Classifier) SerializationHelper.read(modelName);
			for (int i = 0; i < pairs.size(); i++) {
				CPair pair = pairs.get(i);
				Instance feat = features.instance(i);
				double[] pred = clf.distributionForInstance(feat);
				predictions.put(pair, pred[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// clustering
		Set<List<Concept>> clusters = clusterer.createClusters(new HashSet<Concept>(repConcepts), predictions);

		// create final cluster and update relations
		this.updateDataStructures(clusters, groups);
		this.clusters = clusters;

		this.parent.log(this, "grouped concepts: " + concepts.size());
		this.parent.log(this, "relations: " + propositions.size());
	}

	private Instances computeFeatures(List<CPair> pairs, String topic) {

		// setup similarities
		List<ConceptSimilarityMeasure> sims = new ArrayList<ConceptSimilarityMeasure>();
		sims.add(new EditDistance());
		sims.add(new JaccardDistance());
		ConfigManager.setSemilarDataRootFolder("../semilar/resources/");
		sims.add(new WordBasedMeasure(WordNetSimilarity.WNSimMeasure.RES));
		sims.add(new WordBasedMeasure(new LSAWordMetric("LSA-MODEL-TASA-LEMMATIZED-DIM300")));
		sims.add(new WordEmbeddingDistance(EmbeddingType.WORD2VEC, 300, true));

		// prepare dataset
		Instances features = this.createDataSet(sims);
		for (CPair pair : pairs) {
			double[] vals = new double[sims.size() + 1];
			Instance instance = new DenseInstance(1.0, vals);
			features.add(instance);
		}

		// compute similarities
		for (int a = 0; a < sims.size(); a++) {
			ConceptSimilarityMeasure sim = sims.get(a);
			this.parent.log(this, "computing features: " + sim.getName());

			// load cache
			String cacheFile = cacheFolder + "/" + sim.getName() + "." + topic + ".fst";
			ObjectDoubleHashMap<StringPair> cache = loadCache(cacheFile, pairs.size());

			// lookup in cache
			List<Pair<CPair, Integer>> toCompute = new ArrayList<Pair<CPair, Integer>>();
			for (int i = 0; i < pairs.size(); i++) {
				CPair pair = pairs.get(i);
				double s = this.getFromCache(cache, pair.c1, pair.c2);
				if (s == -1)
					toCompute.add(new Pair<CPair, Integer>(pair, i));
				else
					features.instance(i).setValue(a, s);
			}
			System.out.println("to compute: " + toCompute.size());

			// compute per instance
			Muter.mute();
			List<Pair<Integer, Double>> results = null;
			if (parallel && !(sim instanceof JaccardDistance || sim instanceof WordBasedMeasure)) {
				results = toCompute.parallelStream().map(
						p -> new Pair<Integer, Double>(p.second(), sim.computeSimilarity(p.first().c1, p.first().c2)))
						.collect(Collectors.toList());
			} else {
				results = toCompute.stream().map(
						p -> new Pair<Integer, Double>(p.second(), sim.computeSimilarity(p.first().c1, p.first().c2)))
						.collect(Collectors.toList());
			}

			for (Pair<Integer, Double> p : results) {
				features.instance(p.first()).setValue(a, p.second());
				if (saveCache) {
					CPair pair = pairs.get(p.first());
					this.addToCache(cache, pair.c1, pair.c2, p.second());
				}
			}
			Muter.unmute();

			// save
			if (saveCache)
				this.saveCache(cache, cacheFile);
		}

		return features;
	}

	private Instances createDataSet(List<ConceptSimilarityMeasure> sims) {

		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		for (ConceptSimilarityMeasure sim : sims)
			atts.add(new Attribute(sim.getName()));

		List<String> classes = new ArrayList<String>();
		classes.add("no merge");
		classes.add("merge");
		atts.add(new Attribute("class", classes));

		Instances data = new Instances("data", atts, 0);
		data.setClassIndex(data.numAttributes() - 1);

		return data;
	}

	private void fixLemmas(Concept c) {
		for (int i = 0; i < c.tokenList.size(); i++) {
			PToken t = c.tokenList.get(i);
			t = NonUIMAPreprocessor.getInstance().lemmatize(t);
		}
	}

	private void updateDataStructures(Set<List<Concept>> clusters, Map<Concept, ConceptGroup> groups) {

		// merge pre-clustering and classifier clustering
		for (List<Concept> cluster : clusters) {
			List<Concept> extra = new ArrayList<Concept>();
			for (Concept c : cluster)
				extra.addAll(groups.get(c).getAll());
			cluster.clear();
			cluster.addAll(extra);
			Collections.sort(cluster);
		}

		// build mapping
		Map<Concept, Concept> conceptMapping = new HashMap<Concept, Concept>();
		for (List<Concept> cluster : clusters) {
			Concept labelConcept = cluster.get(0);
			conceptMapping.put(labelConcept, labelConcept);
			for (Concept otherConcept : cluster.subList(0, cluster.size())) {
				conceptMapping.put(otherConcept, labelConcept);
			}
		}

		// update concepts
		this.concepts.clear();
		for (List<Concept> cluster : clusters)
			this.concepts.add(cluster.get(0));

		// adapt propositions
		List<Proposition> updated = new ArrayList<Proposition>();
		for (Proposition p : this.propositions) {
			p.sourceConcept = conceptMapping.get(p.sourceConcept);
			p.targetConcept = conceptMapping.get(p.targetConcept);
			if (p.sourceConcept != p.targetConcept)
				updated.add(p);
		}
		this.propositions = updated;
	}

	private List<CPair> buildPairs(List<Concept> concepts) {
		List<CPair> pairs = new ArrayList<CPair>();
		for (int i = 0; i < concepts.size(); i++) {
			for (int j = i + 1; j < concepts.size(); j++) {
				CPair pair = new CPair(concepts.get(i), concepts.get(j));
				pairs.add(pair);
			}
		}
		return pairs;
	}

	private double getFromCache(ObjectDoubleHashMap<StringPair> cache, Concept c1, Concept c2) {
		StringPair cacheKey = new StringPair(c1.name.toLowerCase().trim(), c2.name.toLowerCase().trim());
		return cache.getOrDefault(cacheKey, -1.0);
	}

	private void addToCache(ObjectDoubleHashMap<StringPair> cache, Concept c1, Concept c2, double d) {
		StringPair cacheKey = new StringPair(c1.name.toLowerCase().trim(), c2.name.toLowerCase().trim());
		cache.put(cacheKey, d);
	}

	private ObjectDoubleHashMap<StringPair> loadCache(String fileName, int maxSize) {
		System.out.println("Loading cache");
		ObjectDoubleHashMap<StringPair> cache = new ObjectDoubleHashMap<StringPair>(maxSize);
		try {
			int fileCounter = 0;
			while (true) {
				File file = new File(fileName + fileCounter);
				if (!file.exists())
					break;
				System.out.println(file.getName());
				FSTObjectInput in = new FSTObjectInput(new FileInputStream(file));
				Object o = null;
				while ((o = in.readObject(CachedSim.class)) != null) {
					CachedSim sim = (CachedSim) o;
					cache.put(sim.pair, sim.sim);
				}
				in.close();
				fileCounter++;
			}
		} catch (FileNotFoundException e) {
			// ok, will return empty map then
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("--- done: " + cache.size());
		return cache;
	}

	private void saveCache(ObjectDoubleHashMap<StringPair> cache, String fileName) {
		System.out.println("Saving cache");
		try {
			int count = 0;
			int fileCounter = 0;
			FSTObjectOutput out = new FSTObjectOutput(new FileOutputStream(fileName + fileCounter));
			for (ObjectDoubleCursor<StringPair> pair : cache) {
				if (count == pairsPerFile) {
					out.writeObject(null, CachedSim.class);
					out.close();
					fileCounter++;
					out = new FSTObjectOutput(new FileOutputStream(fileName + fileCounter));
					count = 0;
				}
				CachedSim sim = new CachedSim(pair.key, pair.value);
				out.writeObject(sim, CachedSim.class);
				count++;
			}
			out.writeObject(null, CachedSim.class);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("--- done: " + cache.size());
	}

}
