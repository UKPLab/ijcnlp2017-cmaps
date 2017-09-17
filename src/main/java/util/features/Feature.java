package util.features;

import java.util.ArrayList;
import java.util.List;

import weka.core.Attribute;

/**
 * Feature as key-value-pair
 * 
 * @author falke
 *
 * @param <T>
 *            type of value - supported: int, double, float, bool, string
 */
public class Feature<T> {

	private String name;
	private T value;

	public Feature(String name, T value) {
		this.name = name;
		this.value = value;
		if (this.value instanceof String)
			this.name += "_" + this.value;
		this.name = this.name.toLowerCase();
	}

	public String getName() {
		return this.name;
	}

	public T getValue() {
		return this.value;
	}

	public double getDoubleValue() {
		if (this.value instanceof Double)
			return ((Double) this.value).doubleValue();
		if (this.value instanceof Float)
			return ((Float) this.value).doubleValue();
		if (this.value instanceof Integer)
			return ((Integer) this.value).doubleValue();
		if (this.value instanceof Boolean)
			return (Boolean) this.value ? 1.0 : 0.0;
		if (this.value instanceof String)
			return 1.0;
		return Double.NaN;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Feature<?>))
			return false;
		else {
			@SuppressWarnings("unchecked")
			Feature<T> other = (Feature<T>) o;
			return this.name.equals(other.getName()) && this.getDoubleValue() == other.getDoubleValue();
		}
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() + 31 * Double.hashCode(this.getDoubleValue());
	}

	@Override
	public String toString() {
		return Double.toString(this.getDoubleValue());
	}

	public static Attribute createWekaAttribute(String name, Class<Object> c) {
		if (c.equals(Double.class) || c.equals(Float.class) || c.equals(Integer.class))
			return new Attribute(name);
		if (c.equals(Boolean.class) || c.equals(String.class))
			return new Attribute(name, createValueListBoolean());
		return null;
	}

	private static List<String> createValueListBoolean() {
		List<String> boolAtt = new ArrayList<String>();
		boolAtt.add("f");
		boolAtt.add("t");
		return boolAtt;
	}

}
