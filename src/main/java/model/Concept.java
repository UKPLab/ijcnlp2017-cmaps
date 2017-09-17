package model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Concept implements Comparable<Concept>, Serializable {

	private static final long serialVersionUID = 1L;
	private static int nextId = 0;

	public int id;
	public String name;
	public List<PToken> tokenList;
	public PToken headToken;
	public int headDepDepth;

	public String type;
	public double confidence;
	public double weight;

	public Concept(String name, List<PToken> tokens, String type, double confidence) {
		this.name = name;
		this.tokenList = tokens;
		this.type = type;
		this.confidence = confidence;
		this.id = nextId++;
	}

	public Concept(String name) {
		this(name, new LinkedList<PToken>(), "", 0.0);
	}

	public int[] getSpan() {
		int[] span = { Integer.MAX_VALUE, -1 };
		for (PToken t : this.tokenList) {
			if (t.start < span[0])
				span[0] = t.start;
			if (t.end > span[1])
				span[1] = t.end;
		}
		return span;
	}

	@Override
	public String toString() {
		return this.name + " (" + this.weight + ")";
	}

	@Override
	public int compareTo(Concept o) {
		int comp = Double.compare(o.weight, this.weight);
		if (comp != 0)
			return comp;
		else {
			comp = Integer.compare(this.name.length(), o.name.length());
			if (comp != 0)
				return comp;
			else
				/*
				 * important for sorted set -> comply with equals
				 * since equals is instance equality, two instances should never
				 * be the same in the comparator as well
				 */
				return Integer.compare(this.hashCode(), o.hashCode());
		}
	}
}