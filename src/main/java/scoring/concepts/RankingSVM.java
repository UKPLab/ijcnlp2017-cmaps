package scoring.concepts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;

public class RankingSVM extends AbstractClassifier {

	private static final long serialVersionUID = 1L;

	private double[] weights;

	public void setWeights(String fileName) {
		File f = new File(fileName);
		try {
			LineIterator i = FileUtils.lineIterator(f);
			List<Double> weights = new ArrayList<Double>();
			while (i.hasNext()) {
				String line = i.next().trim();
				if (line.length() > 0)
					weights.add(Double.parseDouble(line));
			}
			this.weights = new double[weights.size()];
			for (int j = 0; j < weights.size(); j++)
				this.weights[j] = weights.get(j);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double scoreInstance(Instance instance) {
		// bias
		double score = 1 * this.weights[0];
		// ignore id and topic and class label
		for (int i = 2; i < instance.numAttributes() - 1; i++) {
			score += this.weights[i - 1] * instance.value(i);
		}
		return score;
	}

	@Override
	public double[][] distributionsForInstances(Instances batch) {

		double[][] dists = new double[batch.numInstances()][2];
		for (int i = 0; i < batch.numInstances(); i++) {
			Instance ins = batch.instance(i);
			dists[i] = new double[2];
			dists[i][1] = this.scoreInstance(ins);
		}

		return dists;
	}

	@Override
	public void buildClassifier(Instances data) throws Exception {
		// nothing - training happens in python
	}

}
