package util.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Feature container - collects features extracted for different instances and
 * then turns them into Weka format
 * 
 * @author falke
 *
 * @param <I>
 *            instance type
 */
public class FeatureContainer<I> {

	private Map<String, Class<Object>> features;
	private TreeSet<String> featuresSorted;
	private Map<I, Map<String, Feature<Object>>> featureValues;

	public FeatureContainer() {
		this.features = new HashMap<String, Class<Object>>();
		this.featuresSorted = new TreeSet<String>();
		this.featureValues = new HashMap<I, Map<String, Feature<Object>>>();
	}

	@SuppressWarnings("unchecked")
	public void add(I key, Feature<? extends Object> f) {

		if (!this.features.containsKey(f.getName())) {
			this.features.put(f.getName(), (Class<Object>) f.getValue().getClass());
			this.featuresSorted.add(f.getName());
		} else {
			Class<Object> type = this.features.get(f.getName());
			if (type != f.getValue().getClass())
				throw new IllegalArgumentException("feature already defined with different type");
		}

		if (!this.featureValues.containsKey(key))
			this.featureValues.put(key, new HashMap<String, Feature<Object>>());
		this.featureValues.get(key).put(f.getName(), (Feature<Object>) f);
	}

	public Collection<Feature<Object>> get(I key) {
		return this.featureValues.get(key).values();
	}

	public List<String> getFeatures() {
		return new ArrayList<String>(this.featuresSorted);
	}

	public Instances createInstances(List<String> orderedFeatureNames) {

		if (orderedFeatureNames == null)
			orderedFeatureNames = new ArrayList<String>(this.getFeatures());

		Instances data = this.createEmptyDataset(orderedFeatureNames);
		for (I key : this.featureValues.keySet())
			data.add(this.createInstance(orderedFeatureNames, key));

		return data;
	}

	public Instance createInstance(List<String> featureNames, I key) {
		double[] vals = new double[featureNames.size()];
		for (int i = 0; i < featureNames.size(); i++) {
			Feature<Object> f = this.featureValues.get(key).get(featureNames.get(i));
			if (f != null)
				vals[i] = f.getDoubleValue();
			else {
				Class<Object> type = features.get(featureNames.get(i));
				if (type.equals(Double.class) || type.equals(Float.class) || type.equals(Integer.class))
					vals[i] = Double.NaN;
				if (type.equals(Boolean.class) || type.equals(String.class))
					vals[i] = 0;
			}
		}
		return new DenseInstance(1.0, vals);
	}

	private Instances createEmptyDataset(List<String> featureNames) {
		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		for (String name : featureNames) {
			if (!this.features.containsKey(name)) {
				Feature<Double> f = new Feature<Double>(name, Double.NaN);
				this.add(this.featureValues.keySet().iterator().next(), f);
			}
			atts.add(Feature.createWekaAttribute(name, this.features.get(name)));
		}
		Instances data = new Instances("data", atts, 0);
		return data;
	}
}
