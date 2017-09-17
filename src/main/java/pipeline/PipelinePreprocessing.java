package pipeline;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasWriter;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser.DependenciesMode;
import preprocessing.CorpusStatWriter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

/**
 * Preprocessing Pipeline
 * 
 * Applies preprocessing tools to all input documents and stores
 * resulting annotations in binary UIMA CAS files for further processing.
 * 
 */
public class PipelinePreprocessing {

	// dataset to be processed
	public static String textFolder = "data/CMapSummaries/dummy";
	public static final String[] textPattern = { "*/*.txt" };

	public static void main(String[] args) throws UIMAException, IOException {

		Logger.getRootLogger().setLevel(Level.INFO);

		// 0) parameter
		if (args.length > 0)
			textFolder = args[0];

		// 1) read text documents
		CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION, textFolder, TextReader.PARAM_PATTERNS, textPattern,
				TextReader.PARAM_LANGUAGE, "en");

		// 2) process documents

		String[] quoteBegin = { "“", "‘" };
		List<String> quoteBeginList = Arrays.asList(quoteBegin);
		String[] quoteEnd = { "”", "’" };
		List<String> quoteEndList = Arrays.asList(quoteEnd);

		// tokenization and sentence splitting
		AnalysisEngineDescription segmenter = AnalysisEngineFactory.createEngineDescription(StanfordSegmenter.class,
				StanfordSegmenter.PARAM_NEWLINE_IS_SENTENCE_BREAK, "ALWAYS");

		// part-of-speech tagging
		AnalysisEngineDescription pos = AnalysisEngineFactory.createEngineDescription(StanfordPosTagger.class,
				StanfordPosTagger.PARAM_QUOTE_BEGIN, quoteBeginList, StanfordPosTagger.PARAM_QUOTE_END, quoteEndList);

		// lemmatizing
		AnalysisEngineDescription lemmatizer = AnalysisEngineFactory.createEngineDescription(StanfordLemmatizer.class,
				StanfordLemmatizer.PARAM_QUOTE_BEGIN, quoteBeginList, StanfordLemmatizer.PARAM_QUOTE_END, quoteEndList);

		// named entity recognition
		AnalysisEngineDescription ner = AnalysisEngineFactory.createEngineDescription(
				StanfordNamedEntityRecognizer.class, StanfordNamedEntityRecognizer.PARAM_QUOTE_BEGIN, quoteBeginList,
				StanfordNamedEntityRecognizer.PARAM_QUOTE_END, quoteEndList);

		// constituency parsing and dependency conversion
		AnalysisEngineDescription parser = AnalysisEngineFactory.createEngineDescription(StanfordParser.class,
				StanfordParser.PARAM_QUOTE_BEGIN, quoteBeginList, StanfordParser.PARAM_QUOTE_END, quoteEndList,
				StanfordParser.PARAM_MODE, DependenciesMode.CC_PROPAGATED);

		// coreference resolution
		AnalysisEngineDescription coref = AnalysisEngineFactory.createEngineDescription();

		// 3) write annotated data to file
		AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(BinaryCasWriter.class,
				BinaryCasWriter.PARAM_TARGET_LOCATION, textFolder, BinaryCasWriter.PARAM_STRIP_EXTENSION, false,
				BinaryCasWriter.PARAM_FILENAME_EXTENSION, ".bin6", BinaryCasWriter.PARAM_OVERWRITE, true);

		// print statistics
		AnalysisEngineDescription stat = AnalysisEngineFactory.createEngineDescription(CorpusStatWriter.class);

		// 4) run pipeline
		SimplePipeline.runPipeline(reader, segmenter, pos, lemmatizer, ner, parser, coref, writer, stat);
	}

}