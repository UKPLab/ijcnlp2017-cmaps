package scoring.concepts.exp;

import scoring.concepts.RankingSVM;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.SerializationHelper;

public class ConvertModel {

	public static void main(String[] args) throws Exception {

		String modelName = "models/wiki_scoring_noun10conjarg2_sim5log-gopt_SVMRankC10_RepDisc";

		String modelFile = modelName + ".model";
		FilteredClassifier clf = (FilteredClassifier) SerializationHelper.read(modelFile);

		RankingSVM svm = new RankingSVM();
		svm.setWeights(modelName + ".weights");

		clf.setClassifier(svm);
		System.out.println(clf);

		SerializationHelper.write(modelName + ".model_new", clf);

	}

}
