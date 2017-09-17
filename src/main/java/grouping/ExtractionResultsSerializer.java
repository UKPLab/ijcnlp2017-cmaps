package grouping;

import java.io.File;

import model.ExtractionResult;
import pipeline.CmmComponent;
import pipeline.Extractor;

public class ExtractionResultsSerializer extends CmmComponent {

	@Override
	public void processCollection() {

		ExtractionResult res = null;

		ConceptGrouperBase exComp = this.parent.getComponent(ConceptGrouperBase.class);
		if (exComp != null) {
			res = new ExtractionResult(exComp.getConcepts(), exComp.getPropositions(), exComp.getClusters());
		} else {
			Extractor ex = this.parent.getPrevExtractor(this);
			res = new ExtractionResult(ex.getConcepts(), ex.getPropositions(), null);
		}

		String fileName = this.parent.getTargetLocation() + "/" + this.parent.getName() + ".groups.ser";
		File file = new File(fileName);
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		ExtractionResult.save(res, fileName);

	}

}
