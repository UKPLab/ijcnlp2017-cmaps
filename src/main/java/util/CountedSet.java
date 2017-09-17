package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CountedSet<T> extends opennlp.tools.util.CountedSet<T> {

	@Override
	public boolean addAll(Collection<? extends T> elems) {
		throw new IllegalArgumentException("addAll not working!");
	}

	public double sum() {
		int sum = 0;
		for (T o : this) {
			sum += this.getCount(o);
		}
		return sum;
	}

	public void setAllCounts(int c) {
		for (T o : this) {
			this.setCount(o, c);
		}
	}

	public Collection<T> getObjects() {
		List<T> objects = new ArrayList<T>();
		for (T o : this)
			objects.add(o);
		return objects;
	}

	public List<Integer> getCounts() {
		List<Integer> counts = new ArrayList<Integer>();
		for (T o : this)
			counts.add(this.getCount(o));
		return counts;
	}

	@Override
	public String toString() {
		String out = "[";
		for (T o : this) {
			out += o + ":" + this.getCount(o) + ", ";
		}
		if (out.length() > 2) {
			out = out.substring(0, out.length() - 2);
		}
		return out + "]";
	}

}
