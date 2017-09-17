package grouping.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.carrotsearch.hppc.ObjectDoubleMap;
import com.carrotsearch.hppc.cursors.ObjectDoubleCursor;

import model.CPair;
import model.Concept;
import util.UnionFind;

/**
 * improves initial solution (connected components) by removing edges, trying
 * each edge after another and removing each that improves the objective
 * 
 * @author falke
 *
 */
public class GreedyLocalOptimizer extends AbstractConceptClusterer {

	protected static final int MAX_ITER = 100000; // all
	protected double posThreshold;

	protected CPair[] allPairs;
	protected double[] predictions;
	protected double fixedScore;
	protected Set<Concept> singleConcepts;
	protected Set<Concept> concepts;
	protected Set<CPair> allEdges;

	public GreedyLocalOptimizer() {
		this(0.5);
	}

	public GreedyLocalOptimizer(double posThreshold) {
		this.posThreshold = posThreshold;
	}

	@Override
	public Set<List<Concept>> createClusters(Set<Concept> concepts, ObjectDoubleMap<CPair> predictions) {

		this.concepts = concepts;

		// initial solution
		Set<CPair> allEdges = this.getPositivePairs(predictions, this.posThreshold);
		this.allEdges = this.transReduction(concepts, allEdges);
		System.out.println("branching factor " + this.allEdges.size());

		Solution sol = new Solution();
		Set<List<Concept>> clusters = this.buildCluster(sol);
		this.precomputeScoring(predictions, clusters);

		sol.score = this.scoreClustering(clusters);
		System.out.println("init " + sol.score);

		// local search
		Random rand = new Random(42);
		List<CPair> edges = new ArrayList<CPair>(this.allEdges);
		Collections.shuffle(edges, rand);

		for (int i = 0; i < edges.size(); i++) {
			// find next edge to remove
			Solution newSolution = null;
			newSolution = buildNeighbour(sol, edges.get(i));
			if (newSolution.score > sol.score) {
				sol = newSolution;
				edges.remove(i);
				i--;
			}
			if (sol.removedEdges.size() == MAX_ITER)
				break;
		}

		// final solution
		clusters = this.buildCluster(sol);

		System.out.println("iterations: " + sol.removedEdges.size() + ", best " + sol.score);

		return clusters;

	}

	protected Solution buildNeighbour(Solution prevSolution, CPair edgeToRemove) {
		Solution newSolution = new Solution(prevSolution, edgeToRemove);
		Set<List<Concept>> clusters = this.buildCluster(newSolution);
		newSolution.score = this.scoreClustering(clusters);
		return newSolution;
	}

	protected Set<List<Concept>> buildCluster(Solution sol) {
		Set<CPair> reducedEdges = new HashSet<CPair>(allEdges);
		reducedEdges.removeAll(sol.removedEdges);
		Set<List<Concept>> clusters = this.buildTransClosureClusters(concepts, reducedEdges);
		return clusters;
	}

	protected Set<CPair> transReduction(Set<Concept> concepts, Set<CPair> pairs) {
		Set<CPair> reduction = new HashSet<CPair>(pairs);
		UnionFind<Concept> unionFind = new UnionFind<Concept>(concepts);
		for (CPair pair : pairs) {
			Concept c1 = unionFind.find(pair.c1);
			Concept c2 = unionFind.find(pair.c2);
			if (c1 == c2)
				reduction.remove(pair);
			else
				unionFind.union(pair.c1, pair.c2);
		}
		return reduction;
	}

	// precompute objective function for pairs that will never change
	// -> single concept clusters
	protected void precomputeScoring(ObjectDoubleMap<CPair> predictions, Set<List<Concept>> clusters) {

		this.singleConcepts = new HashSet<Concept>();
		for (List<Concept> cluster : clusters)
			if (cluster.size() == 1)
				this.singleConcepts.add(cluster.get(0));

		this.fixedScore = 0;
		List<CPair> pairs = new ArrayList<CPair>();
		for (ObjectDoubleCursor<CPair> p : predictions) {
			if (this.singleConcepts.contains(p.key.c1) || this.singleConcepts.contains(p.key.c2)) {
				this.fixedScore += 1 - p.value;
			} else {
				pairs.add(p.key);
			}
		}

		this.allPairs = new CPair[pairs.size()];
		this.predictions = new double[pairs.size()];
		for (int i = 0; i < pairs.size(); i++) {
			this.allPairs[i] = pairs.get(i);
			this.predictions[i] = predictions.get(this.allPairs[i]);
		}
	}

	protected double scoreClustering(Set<List<Concept>> clusters) {
		double score = this.fixedScore;
		Set<CPair> pairs = this.convertClusters(clusters);
		for (int i = 0; i < allPairs.length; i++) {
			if (pairs.contains(allPairs[i]))
				score += predictions[i];
			else
				score += (1 - predictions[i]);
		}
		return score;
	}

	private class Solution implements Comparable<Solution> {
		public Set<CPair> removedEdges;
		public double score;

		public Solution() {
			this.removedEdges = new HashSet<CPair>();
		}

		public Solution(Solution other, CPair newEdge) {
			this.removedEdges = new HashSet<CPair>(other.removedEdges);
			this.removedEdges.add(newEdge);
		}

		@Override
		public int compareTo(Solution o) {
			return Double.compare(o.score, this.score);
		}
	}
}
