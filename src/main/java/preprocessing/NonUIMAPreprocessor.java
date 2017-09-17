package preprocessing;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.MorphaAnnotator;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;
import model.Concept;
import model.PToken;
import semilar.data.Sentence;
import semilar.data.Word;
import util.Stopwords;

/**
 * Standalone preprocessing for data not part of UIMA Pipeline
 * 
 * @author falke
 *
 */
public class NonUIMAPreprocessor {

	private static NonUIMAPreprocessor instance;

	private CRFClassifier<CoreLabel> ner;
	private MorphaAnnotator lemmatizer;
	private MaxentTagger tagger;
	private TokenizerFactory<CoreLabel> tokFactory;
	private Stopwords sw;
	private Set<String> posPuncSet;

	private NonUIMAPreprocessor() {
		this.tokFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		this.lemmatizer = new MorphaAnnotator(false);
		this.sw = new Stopwords("lists/stopwords_en_semilar.txt");
		this.posPuncSet = new HashSet<String>(Arrays.asList(posPunc));
	}

	public static NonUIMAPreprocessor getInstance() {
		if (instance == null)
			instance = new NonUIMAPreprocessor();
		return instance;
	}

	public Concept preprocess(Concept c) {

		if (this.tagger == null)
			this.tagger = new MaxentTagger("ext_models/pos_tagger/english-left3words-distsim.tagger");
		if (this.ner == null)
			this.ner = CRFClassifier.getClassifierNoExceptions("ext_models/ner/english.all.3class.distsim.crf.ser.gz");

		List<CoreLabel> words = tokFactory.getTokenizer(new StringReader(c.name)).tokenize();
		tagger.tagCoreLabels(words);
		words = ner.classifySentence(words);
		words = this.addLemmas(words);

		List<PToken> tokens = new ArrayList<PToken>();
		for (CoreLabel word : words) {
			PToken t = new PToken(word.originalText());
			t.pos = word.tag();
			t.neTag = word.get(CoreAnnotations.AnswerAnnotation.class);
			t.lemma = word.get(LemmaAnnotation.class);
			tokens.add(t);
		}
		c.tokenList = tokens;

		return c;
	}

	private List<CoreLabel> addLemmas(List<CoreLabel> words) {

		Annotation sent = new Annotation("");
		sent.set(TokensAnnotation.class, words);
		List<CoreMap> sentences = new ArrayList<>();
		sentences.add(sent);
		Annotation document = new Annotation("");
		document.set(SentencesAnnotation.class, sentences);

		lemmatizer.annotate(document);

		return words;
	}

	public PToken lemmatize(PToken t) {

		List<CoreLabel> words = tokFactory.getTokenizer(new StringReader(t.text.toLowerCase())).tokenize();
		if (words.size() > 1)
			return t;

		words.get(0).setTag(t.pos);
		if (t.pos.startsWith("N") && t.pos.contains("P")) {
			String tag = t.pos.replace("P", "");
			if (t.text.toLowerCase().charAt(t.text.length() - 1) == 's')
				tag = "NNS";
			words.get(0).setTag(tag);
		}

		words = this.addLemmas(words);
		t.lemma = words.get(0).get(LemmaAnnotation.class);

		return t;
	}

	public Sentence getSemilarSentence(Concept c) {

		Sentence sentence = new Sentence();
		sentence.setRawForm(c.name);

		ArrayList<Word> words = new ArrayList<Word>();
		for (PToken t : c.tokenList) {
			Word word = new Word();
			word.setRawForm(t.text.toLowerCase());
			word.setBaseForm(t.lemma.toLowerCase());
			word.setPos(t.pos);
			word.setIsPunctuaton(posPuncSet.contains(t.pos));
			word.setIsStopWord(sw.isSW(t.text.toLowerCase()));
			words.add(word);
		}
		sentence.setWords(words);

		return sentence;
	}

	private static final String[] posPunc = { "!", "#", "$", "''", "(", ")", ",", "-LRB-", "-RRB-", ".", ":", "?",
			"``" };
}
