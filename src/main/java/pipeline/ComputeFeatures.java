package pipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import grouping.clf.sim.ConceptSimilarityMeasure;
import grouping.clf.sim.JaccardDistance;
import model.Concept;
import model.ConceptMap;
import model.ExtractionResult;
import model.io.ConceptMapReader;
import model.io.Format;
import scoring.concepts.features.FeatureExtractor;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * Compute features for scoring model
 * 
 * Two modes:
 * test=true -> extract features for existing, trained model. needs existing
 * feature extractor (*.fe.ser file)
 * test=false -> extract features to train a new model
 * 
 * @author falke
 *
 */
public class ComputeFeatures {

	public static final String folderName = "data/CMapSummaries/dummy";
	public static final String name = "concept-graph";
	public static final boolean test = true;
	public static final String featureExtractorPath = "models/scoring_noun10conjarg2_sim5log-gopt.fe.ser";

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		Logger.getRootLogger().setLevel(Level.INFO);

		FeatureExtractor ex = null;
		if (!test) {
			ex = new FeatureExtractor();
		} else {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(featureExtractorPath));
			ex = (FeatureExtractor) in.readObject();
			in.close();
		}
		ex.init(folderName, name + ".graph_features.tsv");

		File folder = new File(folderName);
		for (File clusterFolder : folder.listFiles()) {
			if (clusterFolder.isDirectory()) {

				int topic = Integer.parseInt(clusterFolder.getName());

				// load concepts
				String serFileName = folderName + "/" + clusterFolder.getName() + "/" + name + ".groups.ser";
				ExtractionResult res = ExtractionResult.load(serFileName);
				System.out.println(clusterFolder.getName() + " " + res.concepts.size());

				// extract features
				ex.collectFeatures(res.groupedConcepts, topic);

				// create gold labels
				Map<Concept, Boolean> labelsBinary = getBinaryLabels(res.concepts, topic, 0.9);
				ex.addLabels(labelsBinary, "label_binary");
			}
		}

		Instances data = ex.getFeatures();
		System.out.println(data.toSummaryString());

		// save data
		String arffName = folderName + "/" + name + ".arff";
		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File(arffName));
		saver.writeBatch();

		if (!test) {
			ex.startTest();
			String fileName = folderName + "/" + name + ".fe.ser";
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
			out.writeObject(ex);
			out.close();
		}

	}

	private static Map<Concept, Boolean> getBinaryLabels(List<Concept> concepts, int topic, double threshold) {

		File goldFile = new File(folderName + "/" + topic + "/" + topic + ".cmap");
		ConceptMap goldMap = ConceptMapReader.readFromFile(goldFile, Format.TSV);

		Map<Concept, Boolean> labels = new HashMap<Concept, Boolean>();
		ConceptSimilarityMeasure sim = new JaccardDistance();

		int posLabels = 0;
		for (Concept c : concepts) {
			boolean label = false;
			for (Concept g : goldMap.getConcepts()) {
				int goldToken = g.name.split("\\s+").length;
				if (sim.computeSimilarity(c, g) > threshold && c.tokenList.size() <= 2 * goldToken) {
					label = true;
					posLabels++;
					break;
				}
			}
			labels.put(c, label);
		}

		System.out.println("pos: " + posLabels + ", total: " + concepts.size());

		return labels;
	}

}
