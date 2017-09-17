package grouping.clustering;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;

import com.carrotsearch.hppc.ObjectDoubleMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import model.CPair;
import model.Concept;

/**
 * creates best clustering given the classifications
 * 
 * finds most probable relation that is an equivalence relation over the
 * concepts
 * 
 * uses IBM CPLEX
 * 
 * @author falke
 *
 */
public class ILPClusterer_Cplex extends AbstractConceptClusterer {

	private ObjectIntMap<CPair> varMap;
	private IloIntVar[] vars;

	@Override
	public Set<List<Concept>> createClusters(Set<Concept> concepts, ObjectDoubleMap<CPair> predictions) {

		// init
		this.varMap = this.initVarMap(predictions);
		IloCplex problem = null;
		try {
			problem = this.createILP(concepts, predictions);
		} catch (IloException e) {
			System.err.println("Error during ILP creation");
			e.printStackTrace();
			return null;
		}

		// solve
		double[] sol = null;
		try {
			problem.setOut(null);
			boolean solved = problem.solve();
			if (!solved) {
				System.err.println("cplex failed");
			}

			sol = problem.getValues(vars);

		} catch (IloException e) {
			System.err.println("Error solving ILP");
			e.printStackTrace();
			return null;
		}

		// create corresponding clusters
		Set<CPair> pairsToMerge = new HashSet<CPair>();
		for (ObjectCursor<CPair> p : predictions.keys()) {
			double merge = sol[varMap.get(p.value)];
			if (merge > 0)
				pairsToMerge.add(p.value);
		}

		Set<List<Concept>> clusters = this.buildTransClosureClusters(concepts, pairsToMerge);

		return clusters;
	}

	/**
	 * creates a mapping between concept pairs and indices used in the ILP
	 * 
	 * @param predictions
	 */
	private ObjectIntMap<CPair> initVarMap(ObjectDoubleMap<CPair> predictions) {
		ObjectIntMap<CPair> varMap = new ObjectIntHashMap<CPair>();
		int x = 0;
		for (ObjectCursor<CPair> p : predictions.keys()) {
			varMap.put(p.value, x);
			x++;
		}
		return varMap;
	}

	/**
	 * defines the ILP for the given concepts and predictions
	 * 
	 * @param concepts
	 *            Set of concepts to be clustered
	 * @param predictions
	 *            Predictions for all pairs of concepts
	 * @return ILP
	 * @throws IloException
	 */
	private IloCplex createILP(Set<Concept> concepts, ObjectDoubleMap<CPair> predictions) throws IloException {

		// define ILP
		int nbVars = varMap.size(); // primary only
		IloCplex problem = new IloCplex();

		/*
		 * variables (all binary)
		 * primary: x_p if pair p used
		 * aux: x_p+n if pair p is not used
		 */
		this.vars = problem.boolVarArray(2 * nbVars);

		/*
		 * objective function
		 * \sum_pairs s(p) * x_p + (1 - s(p)) (1 - x_p)
		 * (1 - x_p) => x_p+n
		 */
		IloLinearNumExpr objFunc = problem.linearNumExpr();
		for (ObjectCursor<CPair> p : predictions.keys()) {
			int i = varMap.get(p.value);
			double v = predictions.get(p.value);
			if (Double.isNaN(v))
				System.out.println("ERROR");
			objFunc.addTerm(v, vars[i]);
			objFunc.addTerm(1 - v, vars[i + nbVars]);
		}
		problem.addMaximize(objFunc);

		/*
		 * constraints - aux variables
		 * -> one and only one of them has to be 1
		 */
		for (ObjectCursor<CPair> p : predictions.keys()) {
			IloLinearIntExpr cons = problem.linearIntExpr();
			int i = varMap.get(p.value);
			cons.addTerm(1, vars[i]);
			cons.addTerm(1, vars[i + nbVars]);
			problem.addEq(cons, 1);
		}

		/*
		 * constraints - transitivity
		 * -> selection of pairs has to be transitive
		 */
		for (Concept ci : concepts) {
			for (Concept cj : concepts) {
				for (Concept ck : concepts) {
					if (ci != cj && ci != ck && cj != ck) {

						try {
							Set<String> triple = new HashSet<String>();
							triple.add(ci.name);
							triple.add(cj.name);
							triple.add(ck.name);

							int pair_ij = varMap.get(new CPair(ci, cj));
							int pair_ik = varMap.get(new CPair(ci, ck));
							int pair_jk = varMap.get(new CPair(cj, ck));

							IloLinearIntExpr cons = problem.linearIntExpr();
							cons.addTerm(-1, vars[pair_ij]);
							cons.addTerm(-1, vars[pair_jk]);
							cons.addTerm(1, vars[pair_ik]);
							problem.addGe(cons, -1);

							cons = problem.linearIntExpr();
							cons.addTerm(-1, vars[pair_ij]);
							cons.addTerm(1, vars[pair_jk]);
							cons.addTerm(-1, vars[pair_ik]);
							problem.addGe(cons, -1);

							cons = problem.linearIntExpr();
							cons.addTerm(1, vars[pair_ij]);
							cons.addTerm(-1, vars[pair_jk]);
							cons.addTerm(-1, vars[pair_ik]);
							problem.addGe(cons, -1);

						} catch (NullPointerException e) {
							// wrong pair ordering
						}
					}
				}
			}
		}

		return problem;
	}

	private boolean isTransitive(double[] ilpSolution, int n) {

		double[][] adjacencyMatrix = new double[n][n];
		int k = 0;
		for (int i = 0; i < n; i++) {
			adjacencyMatrix[i][i] = 1;
			for (int j = i + 1; j < n; j++) {
				adjacencyMatrix[i][j] = ilpSolution[k];
				adjacencyMatrix[j][i] = ilpSolution[k];
				k++;
			}
		}

		INDArray m = new NDArray(adjacencyMatrix);
		INDArray m2 = m.mmul(m);

		System.out.println(m);

		for (int i = 0; i < m.rows(); i++) {
			for (int j = 0; j < m.columns(); j++) {
				if (m2.getDouble(i, j) > 0 && m.getDouble(i, j) == 0) {
					System.out.println(i + " " + j + " " + m2.getDouble(i, j) + " " + m.getDouble(i, j));
					return false;
				}
			}
		}

		return true;
	}
}
