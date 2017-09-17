package extraction;

import java.util.List;

import model.ConceptMap;
import model.Proposition;
import pipeline.CmmComponent;
import pipeline.Extractor;

public class AllPropsMap extends CmmComponent {

	private ConceptMap map;

	@Override
	public void processCollection() {

		this.map = new ConceptMap(this.parent.getName());

		Extractor exComp = this.parent.getPrevExtractor(this);
		List<Proposition> props = exComp.getPropositions();

		for (Proposition p : props) {
			this.map.addConcept(p.sourceConcept);
			this.map.addConcept(p.targetConcept);
			this.map.addProposition(p);
		}

	}

	@Override
	public ConceptMap getConceptMap() {
		return this.map;
	}

}
