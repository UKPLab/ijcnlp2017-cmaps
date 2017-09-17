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

/*
 * Takes OpenIE-processed documents as input, creates concepts and relations 
 * from them and connects them to a graph based on concept coreferences.
 */
public class PipelineGraph {

	public static final String dataFolder = "data/CMapSummaries/dummy";
	public static final String mapFolder = "data/CMapSummaries/dummy";
	public static final String mapName = "concept-graph";

	public static final String textPattern = "*.oie.bin6";

	public static void main(String[] args) throws UIMAException, IOException {

		// iterate over topics
		File folder = new File(dataFolder);
		for (File clusterFolder : folder.listFiles()) {
			if (clusterFolder.isDirectory()) {

				System.out.println("------------------------------------------------------------");
				System.out.println(clusterFolder.getName());
				System.out.println("------------------------------------------------------------");

				// read preprocessed documents
				String docLocation = dataFolder + "/" + clusterFolder.getName();

				CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
						BinaryCasReader.class, BinaryCasReader.PARAM_SOURCE_LOCATION, docLocation,
						BinaryCasReader.PARAM_PATTERNS, textPattern, BinaryCasReader.PARAM_LANGUAGE, "en");

				// configure concept mapping pipeline
				String[] pipeline = { "extraction.PropositionExtractor", "grouping.ConceptGrouperSimLog",
						"grouping.ExtractionResultsSerializer" };
				String targetLocation = mapFolder + "/" + clusterFolder.getName();

				AnalysisEngineDescription cmm = AnalysisEngineFactory.createEngineDescription(ConceptMapMining.class,
						ConceptMapMining.PARAM_TARGET_LOCATION, targetLocation, ConceptMapMining.PARAM_COMPONENTS,
						pipeline, ConceptMapMining.PARAM_NAME, mapName);

				// run pipeline
				SimplePipeline.runPipeline(reader, cmm);
			}
		}

	}
}