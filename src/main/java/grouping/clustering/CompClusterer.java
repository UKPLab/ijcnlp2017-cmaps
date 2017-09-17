package grouping.clustering;

import java.util.List;
import java.util.Set;

import com.carrotsearch.hppc.ObjectDoubleMap;

import model.CPair;
import model.Concept;

/**
 * makes all possible merges
 * 
 * i.e. clusters are equivalence classes induced by the transitive closure of
 * positive classifications
 * 
 * equivalent: finds connected components in the graph
 * 
 * @author falke
 *
 */
public class CompClusterer extends AbstractConceptClusterer {

	protected double posThreshold = 0.5;

	public CompClusterer() {
		this(0.5);
	}

	public CompClusterer(double posThreshold) {
		this.posThreshold = posThreshold;
	}

	@Override
	public Set<List<Concept>> createClusters(Set<Concept> concepts, ObjectDoubleMap<CPair> predictions) {

		Set<CPair> positivePairs = this.getPositivePairs(predictions, this.posThreshold);
		Set<List<Concept>> clusters = this.buildTransClosureClusters(concepts, positivePairs);

		return clusters;
	}

}
