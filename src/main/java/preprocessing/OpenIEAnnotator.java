package preprocessing;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticArgument;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticPredicate;
import edu.knowitall.collection.immutable.Interval;
import edu.knowitall.openie.Argument;
import edu.knowitall.openie.Extraction;
import edu.knowitall.openie.Instance;
import edu.knowitall.openie.OpenIE;
import edu.knowitall.openie.Relation;
import edu.knowitall.tool.parse.ClearParser;
import edu.knowitall.tool.postag.ClearPostagger;
import edu.knowitall.tool.srl.ClearSrl;
import edu.knowitall.tool.tokenize.ClearTokenizer;
import scala.collection.JavaConversions;

/**
 * Runs Open IE 4 on every sentence and adds corresponding annotations to the
 * CAS containing the result
 * 
 * Open IE data is stored in annotations as follows:
 * 
 * SemanticPredicate -> annotated on relation span
 * - category -> Confidence;Relation;Spans;Context
 * 
 * SemanticArgument -> annotated on argument span
 * - role -> Argument;Spans;Type
 * 
 */
public class OpenIEAnnotator extends JCasAnnotator_ImplBase {

	private OpenIE openIE;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		boolean binary = true;
		ClearParser parser = new ClearParser(new ClearPostagger(new ClearTokenizer()));
		ClearSrl srl = new ClearSrl();

		getLogger().log(Level.INFO, "Loading Open IE");
		openIE = new OpenIE(parser, srl, binary, false);
		getLogger().log(Level.INFO, "Open IE ready");
	};

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		// process sentence by sentence
		for (Sentence sent : JCasUtil.select(jCas, Sentence.class)) {
			String s = sent.getCoveredText();

			// apply Open IE system
			List<Instance> extractionsList = JavaConversions.seqAsJavaList(openIE.extract(s));

			// process extractions
			for (Instance instance : extractionsList) {

				Extraction extraction = instance.extraction();
				boolean valid = true;

				// relation phrase
				Relation relation = extraction.rel();
				String relData = instance.confidence() + ";" + relation.text() + ";";
				int start = Integer.MAX_VALUE;
				int end = -1;
				for (Interval span : JavaConversions.seqAsJavaList(relation.offsets())) {
					relData += span.start() + "-" + span.end() + "|";
					if (span.start() < start)
						start = span.start();
					if (span.end() > end)
						end = span.end();
				}
				relData = relData.substring(0, relData.length() - 1) + ";" + extraction.context();

				SemanticPredicate predicate = new SemanticPredicate(jCas);
				predicate.setCategory(relData);
				predicate.setBegin(sent.getBegin() + start);
				predicate.setEnd(sent.getBegin() + end);
				if (predicate.getEnd() > sent.getEnd())
					valid = false;
				List<SemanticArgument> arguments = new LinkedList<>();

				if (valid && !predicate.getCoveredText().equals(relation.text())) {
					// check is too hard, will fail whenever Open IE uses
					// discontinous span
					// getLogger().log(Level.WARNING, "relation mismatch: " +
					// predicate.getCoveredText() + " <-> " + relation.text());
				}

				// arguments
				for (int i = 0; i < 2; i++) {

					Argument arg = extraction.arg1();
					if (i == 1) {
						if (extraction.arg2s().size() == 0) {
							valid = false;
						} else if (extraction.arg2s().size() > 1) {
							getLogger().log(Level.WARNING, "number of arg2: " + extraction.arg2s().size());
						} else
							arg = JavaConversions.seqAsJavaList(extraction.arg2s()).get(0);
					}

					String argData = arg.text() + ";";
					start = Integer.MAX_VALUE;
					end = -1;
					for (Interval span : JavaConversions.seqAsJavaList(arg.offsets())) {
						argData += span.start() + "-" + span.end() + "|";
						if (span.start() < start)
							start = span.start();
						if (span.end() > end)
							end = span.end();
					}
					argData = argData.substring(0, argData.length() - 1) + ";" + arg.getClass().getSimpleName();

					SemanticArgument argument = new SemanticArgument(jCas);
					argument.setRole(argData);
					argument.setBegin(sent.getBegin() + start);
					argument.setEnd(sent.getBegin() + end);
					if (argument.getEnd() > sent.getEnd())
						valid = false;
					arguments.add(argument);

					if (valid && !argument.getCoveredText().equals(arg.text())) {
						// check is too hard, will fail whenever Open IE uses
						// discontinous span
						// getLogger().log(Level.WARNING, "argument mismatch: "
						// + argument.getCoveredText() + " <-> " + arg.text());
					}
				}

				if (valid) {
					predicate.setArguments(FSCollectionFactory.createFSArray(jCas, arguments));
					predicate.addToIndexes();
					for (SemanticArgument arg : arguments)
						arg.addToIndexes();
				}
			}

		}

	}

}