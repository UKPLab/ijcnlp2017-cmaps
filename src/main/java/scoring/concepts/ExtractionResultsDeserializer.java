package scoring.concepts;

import java.util.List;

import model.Concept;
import model.ExtractionResult;
import model.Proposition;
import pipeline.CmmComponent;
import pipeline.Extractor;

public class ExtractionResultsDeserializer extends CmmComponent implements Extractor {

	public static final String name = "concept-graph";

	protected List<Concept> concepts;
	protected List<Proposition> propositions;

	@Override
	public void processCollection() {

		String fileName = this.parent.getTargetLocation() + "/" + name + ".groups.ser";
		ExtractionResult res = ExtractionResult.load(fileName);

		this.concepts = res.concepts;
		this.propositions = res.propositions;

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
