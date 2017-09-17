package util;

import java.io.Serializable;

public class StringPair implements Serializable {

	private static final long serialVersionUID = 1L;

	public String s1;
	public String s2;

	public StringPair(String s1, String s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof StringPair))
			return false;
		else {
			StringPair sp = (StringPair) o;
			return (this.s1.equals(sp.s1) && this.s2.equals(sp.s2)) || (this.s1.equals(sp.s2) && this.s2.equals(sp.s1));
		}
	}

	@Override
	public int hashCode() {
		return this.s1.hashCode() * this.s2.hashCode();
	}

	@Override
	public String toString() {
		return s1 + " <-> " + s2;
	}
}
