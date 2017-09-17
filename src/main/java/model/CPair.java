package model;

public class CPair {

	public Concept c1;
	public Concept c2;
	private int hashcode;

	public CPair(Concept c1, Concept c2) {
		this.c1 = c1;
		this.c2 = c2;
		this.hashcode = this.computeHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CPair))
			return false;
		else {
			CPair oP = (CPair) o;
			return (this.c1 == oP.c1 && this.c2 == oP.c2) || (this.c1 == oP.c2 && this.c2 == oP.c2);
		}
	}

	@Override
	public int hashCode() {
		return this.hashcode;
	}

	private int computeHashCode() {
		int h1 = this.c1 != null ? this.c1.hashCode() : 42;
		int h2 = this.c2 != null ? this.c2.hashCode() : 42;
		return h1 * h2;
	}
}
