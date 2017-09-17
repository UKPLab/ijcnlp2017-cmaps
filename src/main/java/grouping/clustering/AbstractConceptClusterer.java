package grouping.clustering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.carrotsearch.hppc.ObjectDoubleMap;
import com.carrotsearch.hppc.cursors.ObjectDoubleCursor;

import model.CPair;
import model.Concept;
import util.UnionFind;

public abstract class AbstractConceptClusterer {

	abstract public Set<List<Concept>> createClusters(Set<Concept> concepts, ObjectDoubleMap<CPair> predictions);

	// compute objective function for a clustering
	protected double scoreClustering(Set<List<Concept>> clusters, CPair[] allPairs, double[] predictions) {
		double score = 0;
		Set<CPair> pairs = this.convertClusters(clusters);
		for (int i = 0; i < allPairs.length; i++) {
			if (pairs.contains(allPairs[i]))
				score += predictions[i];
			else
				score += (1 - predictions[i]);
		}
		return score;
	}

	// covert from list of elements for set of paired concepts
	protected Set<CPair> convertClusters(Set<List<Concept>> clusters) {
		Set<CPair> pairs = new HashSet<CPair>();
		for (List<Concept> cluster : clusters) {
			if (cluster.size() > 1) {
				for (Concept c1 : cluster) {
					for (Concept c2 : cluster) {
						if (c1 != c2) {
							pairs.add(new CPair(c1, c2));
							pairs.add(new CPair(c2, c1));
						}
					}
				}
			}
		}
		return pairs;
	}

	// build clusters as transitive closure over given pairs
	protected Set<List<Concept>> buildTransClosureClusters(Set<Concept> concepts, Set<CPair> pairs) {

		UnionFind<Concept> unionFind = new UnionFind<Concept>(concepts);
		for (CPair pair : pairs)
			unionFind.union(pair.c1, pair.c2);

		Set<List<Concept>> clusters = new HashSet<List<Concept>>();
		for (Set<Concept> set : unionFind.getSets())
			clusters.add(new ArrayList<Concept>(set));

		return clusters;
	}

	// return set of all positive classifications
	protected Set<CPair> getPositivePairs(ObjectDoubleMap<CPair> predictions, double threshold) {
		Set<CPair> pairs = new HashSet<CPair>();
		for (ObjectDoubleCursor<CPair> c : predictions) {
			if (c.value >= threshold)
				pairs.add(c.key);
		}
		return pairs;
	}

}
