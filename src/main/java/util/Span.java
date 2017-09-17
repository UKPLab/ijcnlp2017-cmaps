package util;

import java.util.Collection;

/**
 * Span in a sequence - start in inclusive, end is exclusive
 * 
 * @author falke
 *
 */
public class Span {

	public int start;
	public int end;

	public Span(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public boolean intersects(Span other) {
		return (this.start < other.end && this.end > other.start);
	}

	public void reduce(Span other) {
		if (this.start < other.start && this.end > other.start)
			this.end = other.start;
		if (this.end > other.end && this.start < other.end)
			this.start = other.end;
	}

	public void extend(Span other) {
		if (this.start > other.start)
			this.start = other.start;
		if (this.end < other.end)
			this.end = other.end;
	}

	@Override
	public String toString() {
		return this.start + "-" + this.end;
	}

	public static boolean intersect(Collection<Span> spans) {
		for (Span s1 : spans) {
			for (Span s2 : spans) {
				if (s1 != s2) {
					if (s1.intersects(s2))
						return true;
				}
			}
		}
		return false;
	}

}
