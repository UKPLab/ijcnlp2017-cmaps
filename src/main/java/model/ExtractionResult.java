package model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import util.ConnectedComponents;
import util.CountedSet;

/**
 * data structure used to persist results of extraction and grouping
 * 
 * @author falke
 *
 */
public class ExtractionResult implements Serializable {

	private static final long serialVersionUID = -2047129957453204084L;

	public List<Concept> concepts;
	public List<Proposition> propositions;
	public Set<List<Concept>> groupedConcepts;

	public ExtractionResult(List<Concept> concepts, List<Proposition> propositions,
			Set<List<Concept>> groupedConcepts) {
		this.concepts = concepts;
		this.propositions = propositions;
		this.groupedConcepts = groupedConcepts;
	}

	public static ExtractionResult load(String fileName) {
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
			ExtractionResult res = (ExtractionResult) in.readObject();
			in.close();
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void save(ExtractionResult res, String fileName) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
			out.writeObject(res);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getClusteringInfo() {

		StringBuffer sb = new StringBuffer();
		sb.append("concept clustering:\n");

		sb.append("mentions:\t");
		if (this.groupedConcepts != null) {
			int mentions = this.groupedConcepts.stream().mapToInt(x -> x.size()).sum();
			sb.append(mentions);
		} else {
			sb.append("none");
		}
		sb.append("\n");

		sb.append("concepts:\t");
		sb.append(this.concepts.size());
		sb.append("\n");

		DescriptiveStatistics lengthStat = new DescriptiveStatistics();
		for (List<Concept> cluster : this.groupedConcepts)
			lengthStat.addValue(cluster.size());
		sb.append("cluster size:\n");
		sb.append(lengthStat);

		return sb.toString();
	}

	public String getGraphInfo() {

		StringBuffer sb = new StringBuffer();
		sb.append("concept graph:\n");

		sb.append("concepts:\t");
		sb.append(this.concepts.size());
		sb.append("\n");
		sb.append("relations:\t");
		sb.append(this.propositions.size());
		sb.append("\n");

		List<Set<Concept>> components = ConnectedComponents.findConnectedComponents(this.concepts, this.propositions);
		sb.append("components:\t" + components.size() + "\n");

		DescriptiveStatistics lengthStat = new DescriptiveStatistics();
		DescriptiveStatistics weightStat = new DescriptiveStatistics();
		for (Set<Concept> component : components) {
			lengthStat.addValue(component.size());
			double weightSum = component.stream().mapToDouble(x -> x.weight).sum();
			weightStat.addValue(weightSum);
		}
		sb.append("component size:\n");
		sb.append(lengthStat);
		sb.append("component weight:\n");
		sb.append(weightStat);

		sb.append("\n");
		CountedSet<CPair> edges = new CountedSet<CPair>();
		for (Proposition p : this.propositions)
			edges.add(new CPair(p.sourceConcept, p.targetConcept));
		DescriptiveStatistics relStat = new DescriptiveStatistics();
		for (CPair edge : edges)
			relStat.addValue(edges.getCount(edge));
		sb.append("edges:\n");
		sb.append(relStat);

		List<CPair> sortedEdges = new ArrayList<CPair>(edges);
		Collections.sort(sortedEdges, (a, b) -> Integer.compare(edges.getCount(b), edges.getCount(a)));
		sb.append("most:\n");
		for (CPair edge : sortedEdges.subList(0, 10))
			sb.append(edges.getCount(edge) + "\t" + edge.c1 + "\t" + edge.c2 + "\n");

		return sb.toString();
	}
}
