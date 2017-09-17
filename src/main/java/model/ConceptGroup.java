package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Group of coreferent concept mentions
 * 
 * @author falke
 *
 */
public class ConceptGroup {

	private TreeSet<Concept> concepts;

	public ConceptGroup() {
		this.concepts = new TreeSet<Concept>();
	}

	public ConceptGroup(Collection<Concept> concepts) {
		this.concepts = new TreeSet<Concept>(concepts);
	}

	public void add(Concept c) {
		this.concepts.add(c);
	}

	public Concept getRep() {
		return this.concepts.first();
	}

	public List<Concept> getAll() {
		return new ArrayList<Concept>(this.concepts);
	}

	public int size() {
		return this.concepts.size();
	}
}
