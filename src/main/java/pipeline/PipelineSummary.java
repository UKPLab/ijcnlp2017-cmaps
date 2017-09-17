package pipeline;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasReader;
import model.ConceptMap;
import model.io.ConceptMapReader;
import model.io.Format;

/*
 * Builds summary concept maps after scoring concepts
 */
public class PipelineSummary {

	public static final String dataFolder = "data/CMapSummaries/dummy";
	public static final String mapFolder = "data/CMapSummaries/dummy";
	public static final String mapName = "concept-graph";

	public static int maxConcepts = 25;

	public static void main(String[] args) throws UIMAException, IOException {

		// iterate over topics
		File folder = new File(dataFolder);
		for (File clusterFolder : folder.listFiles()) {
			if (clusterFolder.isDirectory()) {

				if (args.length > 0 && !clusterFolder.getName().equals(args[0]))
					continue;

				System.out.println("------------------------------------------------------------");
				System.out.println(clusterFolder.getName());
				System.out.println("------------------------------------------------------------");

				// read preprocessed documents
				String docLocation = dataFolder + "/" + clusterFolder.getName();

				// only dummy here!
				CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
						BinaryCasReader.class, BinaryCasReader.PARAM_SOURCE_LOCATION, docLocation,
						BinaryCasReader.PARAM_PATTERNS, "", BinaryCasReader.PARAM_LANGUAGE, "en");

				// configure concept mapping pipeline
				String[] pipeline = { "scoring.concepts.ExtractionResultsDeserializer",
						"scoring.concepts.ConceptScorerRanking", "mapbuilding.MapBuilderILP" };
				String targetLocation = mapFolder + "/" + clusterFolder.getName();

				// determine target size
				File goldFile = new File(docLocation + "/" + clusterFolder.getName() + ".cmap");
				ConceptMap goldMap = ConceptMapReader.readFromFile(goldFile, Format.TSV);
				maxConcepts = goldMap.getConcepts().size();

				AnalysisEngineDescription cmm = AnalysisEngineFactory.createEngineDescription(ConceptMapMining.class,
						ConceptMapMining.PARAM_TARGET_LOCATION, targetLocation, ConceptMapMining.PARAM_COMPONENTS,
						pipeline, ConceptMapMining.PARAM_MAX_CONCEPTS, maxConcepts, ConceptMapMining.PARAM_NAME,
						mapName, ConceptMapMining.PARAM_PRINT_MAPS, false);

				// run pipeline
				SimplePipeline.runPipeline(reader, cmm);
			}
		}

	}
}