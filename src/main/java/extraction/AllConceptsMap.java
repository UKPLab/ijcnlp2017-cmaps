package extraction;

import java.util.Collections;
import java.util.List;

import model.Concept;
import model.ConceptMap;
import model.Proposition;
import pipeline.CmmComponent;
import pipeline.Extractor;

public class AllConceptsMap extends CmmComponent {

	private ConceptMap map;

	@Override
	public void processCollection() {

		this.map = new ConceptMap(this.parent.getName());

		Extractor exComp = this.parent.getPrevExtractor(this);
		List<Concept> concepts = exComp.getConcepts();
		Collections.sort(concepts);

		for (Concept c : concepts.subList(0, this.parent.getMaxConcepts())) {
			this.map.addConcept(c);
			this.map.addProposition(new Proposition(c, c, "dummy"));
		}

	}

	@Override
	public ConceptMap getConceptMap() {
		return this.map;
	}

}
