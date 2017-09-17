package extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.CONJ;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PUNC;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticArgument;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticPredicate;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import edu.stanford.nlp.util.Triple;
import model.Concept;
import model.PToken;
import model.Proposition;
import pipeline.CmmComponent;
import pipeline.Extractor;
import util.Span;

/**
 * Extracts Concepts and Relations based on Open IE Annotations
 * 
 * - resolves pronoun-args based on coref annotations
 */
public class PropositionExtractor extends CmmComponent implements Extractor {

	private static boolean mustHaveNoun = true;
	private static int maxLength = 10;
	private static boolean useTokensAsLabel = false;
	private static boolean applyRuleConj = true;
	private static boolean applyRuleArg2 = true;
	private static boolean applyRuleAux = false; // no effect

	protected List<Concept> concepts;
	protected List<Proposition> propositions;

	private Map<CoreferenceLink, CoreferenceLink> corefMap;
	private String corefDocId;

	public PropositionExtractor() {
		this.concepts = new ArrayList<Concept>();
		this.propositions = new ArrayList<Proposition>();
	};

	@Override
	public void processCollection() {

		// join duplicate concepts (two objects for same span in same doc)
		Map<String, Concept> span2Concept = new HashMap<String, Concept>();
		Map<Concept, Concept> conceptMapping = new HashMap<Concept, Concept>();
		Set<Concept> cSet = new HashSet<Concept>(this.concepts);
		for (Concept c : cSet) {
			PToken firstToken = c.tokenList.get(0);
			PToken lastToken = c.tokenList.get(c.tokenList.size() - 1);
			String span = firstToken.documentId + "-" + firstToken.start + ":" + lastToken.end;
			if (span2Concept.containsKey(span))
				conceptMapping.put(c, span2Concept.get(span));
			else
				span2Concept.put(span, c);
		}
		this.concepts = new ArrayList<Concept>(span2Concept.values());

		for (Proposition p : this.propositions) {
			if (conceptMapping.containsKey(p.sourceConcept))
				p.sourceConcept = conceptMapping.get(p.sourceConcept);
			if (conceptMapping.containsKey(p.targetConcept))
				p.targetConcept = conceptMapping.get(p.targetConcept);
		}

		this.parent.log(this, "concept extraction: " + this.concepts.size());
		this.parent.log(this, "relation extraction: " + this.propositions.size());
	}

	@Override
	public void processSentence(JCas jcas, Sentence sent) {
		this.buildCorefMap(jcas);

		// for each Open IE tuple
		for (SemanticPredicate relation : JCasUtil.selectCovered(SemanticPredicate.class, sent)) {

			String[] info = relation.getCategory().split(";");
			double conf = Double.parseDouble(info[0]);

			Collection<Concept> sConcepts = this.createConcept(jcas, relation.getArguments(0), conf, sent);
			Collection<Concept> tConcepts = this.createConcept(jcas, relation.getArguments(1), conf, sent);

			for (Concept sConcept : sConcepts) {
				if (sConcept != null) {
					for (Concept tConcept : tConcepts) {
						if (sConcept != null && sConcept != tConcept) {

							Proposition p = this.createProposition(relation, sConcept, tConcept, conf, sent, jcas);
							if (p != null) {
								this.concepts.add(p.sourceConcept);
								this.concepts.add(p.targetConcept);
								this.propositions.add(p);

								if (applyRuleArg2) {
									Proposition cp = this.correctProposition(p, sent, jcas);
									if (cp != null) {
										this.concepts.add(cp.sourceConcept);
										this.concepts.add(cp.targetConcept);
										this.propositions.add(cp);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * create a concept from an Open IE argument
	 */
	public Collection<Concept> createConcept(JCas jcas, SemanticArgument arg, double conf, Sentence sent) {
		Collection<Concept> concepts = new LinkedList<Concept>();

		String[] info = arg.getRole().split(";");
		String type = info[info.length - 1];

		Triple<List<PToken>, PToken, Integer> tokens = this.createTokenList(arg, sent, false);
		if (tokens == null || tokens.first().size() == 0)
			return concepts;
		if (info[0].toLowerCase().trim().length() == 0 || useTokensAsLabel) {
			String label = tokens.first().stream().map(x -> x.text.toLowerCase()).collect(Collectors.joining(" "));
			info[0] = label;
		}

		// try to split conjunctions in this argument
		if (applyRuleConj) {
			concepts = this.breakDownConjunction(jcas, sent, arg, conf);
			if (concepts.size() > 0)
				return concepts;
		}

		// try to replace pronoun arguments with coreferent NPs
		if (tokens.first().size() == 1 && tokens.first().get(0).pos.startsWith("P")
				&& JCasUtil.selectCovered(CoreferenceLink.class, arg).size() > 0) {
			CoreferenceLink link = JCasUtil.selectCovered(CoreferenceLink.class, arg).get(0);
			CoreferenceLink head = this.corefMap.get(link);
			if (head != null) {
				Triple<List<PToken>, PToken, Integer> antTokens = this.createTokenList(head, sent, false);
				if (antTokens != null && antTokens.first().size() > 0) {
					info[0] = head.getCoveredText();
					tokens = antTokens;
				}
			}
		}

		// check constraints
		if (maxLength > 0) {
			if (tokens.first().size() > maxLength)
				return concepts;
		}
		if (mustHaveNoun) {
			boolean hasNoun = false;
			for (PToken t : tokens.first())
				if (t.pos.startsWith("N"))
					hasNoun = true;
			if (!hasNoun)
				return concepts;
		}

		// create concept
		Concept concept = new Concept(info[0].toLowerCase().trim(), tokens.first(), type, conf);
		concept.headToken = tokens.second();
		concept.headDepDepth = tokens.third();
		concepts.add(concept);

		return concepts;
	}

	/**
	 * create a proposition from an Open IE triple
	 */
	public Proposition createProposition(SemanticPredicate pred, Concept source, Concept target, double conf,
			Sentence sent, JCas jcas) {

		String[] info = pred.getCategory().split(";");

		Triple<List<PToken>, PToken, Integer> tokens = this.createTokenList(pred, sent, true);
		if (tokens == null || tokens.first().size() == 0)
			return null;

		boolean auxApplied = false;
		if (applyRuleAux) {
			// find all dependents of tokens
			Set<Token> allTokens = new HashSet<Token>();
			int left = source.tokenList.stream().mapToInt(t -> t.end).max().getAsInt();
			int right = target.tokenList.stream().mapToInt(t -> t.start).min().getAsInt();
			for (PToken pt : tokens.first()) {
				Token t = JCasUtil.selectCovered(jcas, Token.class, pt.start, pt.end).get(0);
				allTokens.add(t);
			}
			Set<Token> newTokens = new HashSet<Token>();
			for (Dependency dep : JCasUtil.selectCovered(Dependency.class, sent)) {
				if (allTokens.contains(dep.getGovernor()) && !allTokens.contains(dep.getDependent())
						&& dep.getDependencyType().startsWith("aux") && dep.getDependent().getBegin() >= left
						&& dep.getDependent().getEnd() <= right) {
					newTokens.add(dep.getDependent());
					auxApplied = true;
					System.out.println(dep.getDependencyType());
				}
			}
			// add them
			for (Token nt : newTokens)
				tokens.first().add(this.parent.getToken(nt));
			Collections.sort(tokens.first(), (a, b) -> Integer.compare(a.start, b.start));
		}

		// label
		String label = info[1].toLowerCase().trim();
		if (label.contains("[")) {
			label = label.replaceAll("[\\[\\]]", "");
		} else if (label.length() == 0 || auxApplied || useTokensAsLabel) {
			label = tokens.first().stream().map(x -> x.text.toLowerCase()).collect(Collectors.joining(" "));
		}

		Proposition prop = new Proposition(source, target, label, tokens.first(), info[info.length - 1], conf);
		prop.headToken = tokens.second();
		prop.headDepDepth = tokens.third();
		return prop;
	}

	// split conjuncts into separate arguments
	protected Collection<Concept> breakDownConjunction(JCas jcas, Sentence sent, SemanticArgument arg, double conf) {
		Collection<Concept> concepts = new LinkedList<Concept>();

		Set<Token> heads = this.getHeadsOfSpan(sent, arg);
		if (heads.size() == 1) {

			Token head = heads.iterator().next();
			Span headSpan = new Span(arg.getBegin(), arg.getEnd());

			List<Span> conjSpans = new ArrayList<Span>();
			for (Dependency dep : JCasUtil.selectCovered(Dependency.class, sent)) {
				// all deps of head
				if (dep.getGovernor() == head && dep.getDependent().getBegin() >= arg.getBegin()
						&& dep.getDependent().getEnd() <= arg.getEnd()) {
					// collect conjuncts
					if (dep.getDependencyType().startsWith("conj")) {
						Span conjSpan = this.findTokDepSpan(sent, dep.getDependent());
						conjSpans.add(conjSpan);
						headSpan.reduce(conjSpan);
					}
				}
			}
			conjSpans.add(headSpan);

			if (conjSpans.size() > 1 && !Span.intersect(conjSpans)) {
				for (Span conjSpan : conjSpans) {

					SemanticArgument dummy = new SemanticArgument(jcas);
					dummy.setBegin(conjSpan.start);
					dummy.setEnd(conjSpan.end);

					String[] infoOld = arg.getRole().split(";");
					dummy.setRole(";;" + infoOld[2]);

					concepts.addAll(this.createConcept(jcas, dummy, conf, sent));
				}
			}
		}
		return concepts;
	}

	// try to correct spans of relation phrase and arg2
	protected Proposition correctProposition(Proposition p, Sentence sent, JCas jcas) {

		List<PToken> relToken = new ArrayList<PToken>(p.relationPhraseToken);
		List<PToken> arg2Token = new ArrayList<PToken>(p.targetConcept.tokenList);

		// if arg2 starts with a verb, move it to the relation phrase
		if (arg2Token.get(0).pos.startsWith("V") && !arg2Token.get(0).pos.equals("VBN")) {

			List<PToken> arg2TokenNew = new ArrayList<PToken>(arg2Token);
			for (PToken t : arg2Token) {
				if (t.pos.startsWith("V") || t.pos.startsWith("I") || t.pos.startsWith("J")) {
					relToken.add(t);
					arg2TokenNew.remove(t);
				} else
					break;
			}

			if (arg2TokenNew.size() > 0) {
				if (!arg2TokenNew.get(0).pos.startsWith("D") && !arg2TokenNew.get(0).pos.startsWith("N")
						&& !arg2TokenNew.get(0).pos.startsWith("J"))
					return null;

				Annotation dummy = new Annotation(jcas, arg2TokenNew.get(0).start,
						arg2TokenNew.get(arg2TokenNew.size() - 1).end);
				Triple<List<PToken>, PToken, Integer> arg2Tokens = this.createTokenList(dummy, sent, false);
				if (arg2Tokens == null || arg2Tokens.first().size() == 0)
					return null;

				String labelArg2 = arg2Tokens.first().stream().map(x -> x.text.toLowerCase())
						.collect(Collectors.joining(" "));
				Concept nArg2 = new Concept(labelArg2, arg2Tokens.first(), p.targetConcept.type,
						p.targetConcept.confidence);
				nArg2.headToken = arg2Tokens.second();
				nArg2.headDepDepth = arg2Tokens.third();

				dummy = new Annotation(jcas, relToken.get(0).start, relToken.get(relToken.size() - 1).end);
				Triple<List<PToken>, PToken, Integer> relTokens = this.createTokenList(dummy, sent, true);
				if (relTokens == null || relTokens.first().size() == 0)
					return null;

				String labelRel = relTokens.first().stream().map(t -> t.text.toLowerCase())
						.collect(Collectors.joining(" "));
				Proposition np = new Proposition(p.sourceConcept, nArg2, labelRel, relTokens.first(), p.context,
						p.confidence);
				np.headToken = relTokens.second();
				np.headDepDepth = relTokens.third();

				return np;
			}
		}
		return null;
	}

	// build lookup map to heads of coreference chains
	protected void buildCorefMap(JCas jcas) {

		if (this.getDocId(jcas).equals(this.corefDocId))
			return;

		this.corefMap = new HashMap<CoreferenceLink, CoreferenceLink>();
		for (CoreferenceChain chain : JCasUtil.select(jcas, CoreferenceChain.class)) {
			List<CoreferenceLink> links = this.getLinks(chain);
			if (links.size() > 1) {
				int i;
				for (i = 0; i < links.size() && links.get(i).getReferenceType().equals("PRONOMINAL"); i++)
					;
				CoreferenceLink head = i < links.size() ? links.get(i) : null;
				if (head != null) {
					for (CoreferenceLink link : links)
						corefMap.put(link, head);
				}
			}
		}
		this.corefDocId = this.getDocId(jcas);
	}

	// collect all references in a coreference chain
	protected List<CoreferenceLink> getLinks(CoreferenceChain chain) {
		List<CoreferenceLink> links = new ArrayList<CoreferenceLink>();
		CoreferenceLink link = chain.getFirst();
		do {
			links.add(link);
			link = link.getNext();
		} while (link != null);
		return links;
	}

	// collect tokens covered by an annotation
	protected Triple<List<PToken>, PToken, Integer> createTokenList(Annotation a, Sentence sent, boolean isProp) {

		// tokens
		List<Token> tokens = JCasUtil.selectCovered(Token.class, a);

		Class[] invStartEndPoS = { PUNC.class, CONJ.class };
		Set<Class> invStartEndPoSSet = new HashSet<Class>(Arrays.asList(invStartEndPoS));

		while (tokens.size() > 0 && invStartEndPoSSet.contains(tokens.get(0).getPos().getClass()))
			tokens = tokens.subList(1, tokens.size());
		while (tokens.size() > 0 && invStartEndPoSSet.contains(tokens.get(tokens.size() - 1).getPos().getClass()))
			tokens = tokens.subList(0, tokens.size() - 1);
		if (tokens.size() == 0)
			return null;

		List<PToken> pTokens = new LinkedList<PToken>();
		Map<Token, PToken> mapping = new HashMap<Token, PToken>();
		for (Token t : tokens) {
			PToken pt = this.parent.getToken(t);
			pTokens.add(pt);
			mapping.put(t, pt);
		}

		// find head
		Set<Token> heads = this.getHeadsOfSpan(sent, a);
		if (heads.size() == 0)
			heads.addAll(tokens);

		// select head from candidates
		Token head = null;
		if (heads.size() == 1) {
			head = heads.iterator().next();
		} else {
			List<Token> filteredHeads = new ArrayList<Token>();
			for (Token t : heads)
				if ((!isProp && t.getPos().getPosValue().startsWith("N"))
						|| (isProp && t.getPos().getPosValue().startsWith("V")))
					filteredHeads.add(t);
			if (filteredHeads.size() == 0)
				filteredHeads.addAll(heads);
			Collections.sort(filteredHeads, new Comparator<Token>() {
				@Override
				public int compare(Token o1, Token o2) {
					return Integer.compare(o2.getBegin(), o1.getBegin());
				}
			});
			head = filteredHeads.get(0);
		}

		// determine path length to root
		Map<Token, Set<Token>> deps = this.buildGovMap(sent);
		int l = this.pathLengthToRoot(head, deps);

		Triple<List<PToken>, PToken, Integer> result = new Triple<List<PToken>, PToken, Integer>(pTokens,
				mapping.get(head), l);
		return result;
	}

	private Set<Token> getHeadsOfSpan(Sentence sent, Annotation a) {

		// map: token -> governor(s)
		Map<Token, Set<Token>> deps = this.buildGovMap(sent);

		// collect all heads (= have governor outside of span)
		Set<Token> heads = new HashSet<Token>();
		List<Token> ts = JCasUtil.selectCovered(Token.class, a);
		if (ts.size() == 1) {
			heads.add(ts.get(0));
		} else {
			for (Token t : ts) {
				Set<Token> visited = new HashSet<Token>();
				Queue<Token> queue = new LinkedList<Token>();
				queue.add(t);
				while (!queue.isEmpty()) {
					Token cur = queue.poll();
					visited.add(cur);
					if (deps.get(cur) != null) {
						if (deps.get(cur).size() == 0) {
							// is root and in span: use this
							if (cur.getBegin() >= a.getBegin() && cur.getEnd() <= a.getEnd())
								heads.add(cur);
						} else {
							// if multiple, prefer governors within the span
							Set<Token> allGovs = deps.get(cur);
							Set<Token> withinSpanGovs = new HashSet<Token>(allGovs);
							for (Token gov : allGovs)
								if (!(gov.getBegin() >= a.getBegin()) || !(gov.getEnd() <= a.getEnd()))
									withinSpanGovs.remove(gov);
							if (withinSpanGovs.isEmpty())
								withinSpanGovs.addAll(allGovs);
							for (Token gov : withinSpanGovs) {
								// governor within span: check it
								if (gov.getBegin() >= a.getBegin() && gov.getEnd() <= a.getEnd()) {
									if (!visited.contains(gov))
										queue.add(gov);
								} else {
									// governor out of span: use this
									heads.add(cur);
								}
							}
						}
					}
				}
			}
		}

		return heads;
	}

	private int pathLengthToRoot(Token t, Map<Token, Set<Token>> deps) {
		Set<Token> visited = new HashSet<Token>();
		return this.pathLengthToRoot(t, deps, visited, 0);
	}

	private int pathLengthToRoot(Token t, Map<Token, Set<Token>> deps, Set<Token> visited, int l) {
		visited.add(t);
		int pl = 0;
		if (deps.get(t) == null || deps.get(t).size() == 0)
			return l;
		for (Token parent : deps.get(t)) {
			if (visited.contains(parent))
				pl += 0;
			else
				pl += this.pathLengthToRoot(parent, deps, visited, l + 1);
		}
		return pl;
	}

	private Span findTokDepSpan(Sentence sent, Token t) {

		// map: token -> dependents
		Map<Token, Set<Token>> deps = new HashMap<Token, Set<Token>>();
		for (Dependency dep : JCasUtil.selectCovered(Dependency.class, sent)) {
			if (!dep.getDependencyType().equals("root")) {
				if (!deps.containsKey(dep.getGovernor()))
					deps.put(dep.getGovernor(), new HashSet<Token>());
				deps.get(dep.getGovernor()).add(dep.getDependent());
			}
		}

		Span span = new Span(Integer.MAX_VALUE, -1);
		Queue<Token> queue = new LinkedList<Token>();
		Set<Token> visited = new HashSet<Token>();
		queue.add(t);
		while (!queue.isEmpty()) {
			Token cur = queue.poll();
			span.extend(new Span(cur.getBegin(), cur.getEnd()));
			visited.add(cur);
			if (deps.get(cur) != null)
				for (Token dep : deps.get(cur))
					if (!visited.contains(dep))
						queue.add(dep);
		}

		return span;
	}

	private Map<Token, Set<Token>> buildGovMap(Sentence sent) {
		// map: token -> governor(s)
		Map<Token, Set<Token>> deps = new HashMap<Token, Set<Token>>();
		for (Dependency dep : JCasUtil.selectCovered(Dependency.class, sent)) {
			if (dep.getDependencyType().equals("root"))
				deps.put(dep.getDependent(), new HashSet<Token>());
			else {
				if (!deps.containsKey(dep.getDependent()))
					deps.put(dep.getDependent(), new HashSet<Token>());
				deps.get(dep.getDependent()).add(dep.getGovernor());
			}
		}
		return deps;
	}

	// get name of current document
	protected String getDocId(JCas jcas) {
		DocumentMetaData meta = (DocumentMetaData) jcas.getDocumentAnnotationFs();
		return meta.getDocumentTitle();
	}

	@Override
	public List<Concept> getConcepts() {
		return this.concepts;
	}

	@Override
	public List<Proposition> getPropositions() {
		return this.propositions;
	}

}