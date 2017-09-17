package model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Proposition implements Serializable {

	private static final long serialVersionUID = 1L;
	private static int nextId = 0;

	public int id;
	public Concept sourceConcept;
	public Concept targetConcept;

	public String relationPhrase;
	public List<PToken> relationPhraseToken;
	public PToken headToken;
	public int headDepDepth;

	public String context;
	public double confidence;
	public double weight;

	public Proposition(Concept source, Concept target, String relation, List<PToken> relationToken, String context,
			double confidence) {
		this.sourceConcept = source;
		this.targetConcept = target;
		this.relationPhrase = relation;
		this.relationPhraseToken = relationToken;
		this.context = context;
		this.confidence = confidence;
		this.id = nextId++;
	}

	public Proposition(Concept source, Concept target, String link) {
		this(source, target, link, new LinkedList<PToken>(), "", 0.0);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Proposition))
			return false;
		else {
			Proposition pO = (Proposition) o;
			return this.sourceConcept == pO.sourceConcept && this.targetConcept == pO.targetConcept
					&& this.relationPhrase == pO.relationPhrase;
		}
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.sourceConcept.name.hashCode();
		hash = hash * 31 + this.targetConcept.name.hashCode();
		hash = hash * 31 + this.relationPhrase.hashCode();
		return hash;
	}

	@Override
	public String toString() {
		return this.sourceConcept + " - " + this.relationPhrase + " - " + this.targetConcept;
	}

}