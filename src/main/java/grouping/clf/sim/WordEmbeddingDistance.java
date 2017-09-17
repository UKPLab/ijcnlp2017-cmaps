package grouping.clf.sim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import model.Concept;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

public class WordEmbeddingDistance extends ConceptSimilarityMeasure {

	private static String word2vecFile = "C:\\Data\\embeddings\\GoogleNews-vectors-negative300.bin.gz";
	private static String gloveFile = "";

	private static EmbeddingType type;
	private static int dimension;
	private static WordVectors wordVectors;
	private static INDArray unkVector;

	public WordEmbeddingDistance(EmbeddingType typ, int dim, boolean lazy) {
		type = typ;
		dimension = dim;
		if (!lazy && wordVectors == null) {
			this.loadWordVectors(type, dimension);
			int[] shape = wordVectors.lookupTable().getWeights().shape();
			System.out.println("word embeddings loaded, " + shape[0] + " " + shape[1]);
		}
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName() + " " + type + " " + dimension + "d";
	}

	@Override
	public double computeSimilarity(Concept c1, Concept c2) {
		if (c1.name.toLowerCase().equals(c2.name.toLowerCase()))
			return 1;

		if (wordVectors == null) {
			this.loadWordVectors(type, dimension);
			int[] shape = wordVectors.lookupTable().getWeights().shape();
			System.out.println("word embeddings loaded, " + shape[0] + " " + shape[1]);
		}

		INDArray cVector1 = this.getConceptVector(c1);
		INDArray cVector2 = this.getConceptVector(c2);
		if (cVector1 == null || cVector2 == null)
			return Double.NaN;

		double dist = Transforms.cosineSim(cVector1, cVector2);

		if (Double.isNaN(dist))
			System.err.println("Embedding NaN");

		return dist;
	}

	public INDArray getConceptVector(Concept c) {

		Tokenizer tok = SimpleTokenizer.INSTANCE;

		List<INDArray> vectors = new ArrayList<INDArray>();
		int countUnk = 0;
		for (String word : tok.tokenize(c.name.toLowerCase().trim())) {
			if (wordVectors.hasWord(word))
				vectors.add(wordVectors.getWordVectorMatrix(word));
			else {
				vectors.add(unkVector);
				countUnk++;
			}
		}
		if (vectors.size() == countUnk)
			return null; // all tokens unknown
		INDArray allVectors = Nd4j.vstack(vectors);

		// sum or mean is irrelevant for cosine similarity
		INDArray conceptVector = allVectors.mean(0);

		return conceptVector;
	}

	public EmbeddingType getType() {
		return type;
	}

	public int getDim() {
		return dimension;
	}

	private void loadWordVectors(EmbeddingType type, int dimension) {

		if (type == EmbeddingType.WORD2VEC) {
			File file = new File(word2vecFile);
			wordVectors = WordVectorSerializer.readWord2VecModel(file);
			dimension = 300;
		} else {
			File file = new File(gloveFile.replace("$dim$", dimension + ""));
			try {
				wordVectors = WordVectorSerializer.loadTxtVectors(file);
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		unkVector = wordVectors.lookupTable().getWeights().mean(0);
	}

	public enum EmbeddingType {
		WORD2VEC, GLOVE
	}

}
