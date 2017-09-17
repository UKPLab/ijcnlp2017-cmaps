package scoring.concepts.exp;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import model.Concept;
import model.ExtractionResult;
import weka.core.Instances;

public class TrainingData implements Serializable {

	private static final long serialVersionUID = 1L;

	public Map<String, ExtractionResult> extractions;
	public Instances features;
	public Map<String, Integer> featureMapping;
	public Map<String, Set<Concept>> goldConcepts;

}
