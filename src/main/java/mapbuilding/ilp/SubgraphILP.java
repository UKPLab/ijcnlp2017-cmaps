package mapbuilding.ilp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import model.CPair;
import model.Concept;
import model.Proposition;

/**
 * 
 * Objective Function:
 * \sum c_i*s_i
 * 
 * Parameter:
 * L - max number of concepts
 * s_i - score for concept i
 * r_ij - existing relation from i to j
 * 
 * Variables:
 * c_i - concept included - binary
 * e_ij - edge from i to j - binary
 * f_ij - flow from i to j - int, >= 0
 * 
 * Constraints:
 * 1) \forall i: \sum c_i <= L (size)
 * 2) \forall ij: e_ij = 0 (edge only where relation)
 * 3) \forall ij: e_ij <= c_i (edge only if source included)
 * 4) \forall ij: e_ij <= c_j (edge only if target included)
 * 5) \sum e_0i = 1 (root link sends flow to only one concept)
 * 6) \sum f_0i - \sum c_i = 0 (root sends flow <= included concepts)
 * 7) \forall k: \sum f_ik - \sum f_kp - c_k = 0 (concepts consumes on flow)
 * 8) \forall ij: C*e_ij - f_ij >= 0 (flow only where edges are present)
 * 
 * @author falke
 *
 */
public class SubgraphILP extends GraphSummarizer {

	private static final boolean DEBUG = false;

	private ObjectIntMap<Concept> conceptIds;
	private Set<CPair> relations;
	private CPair[] edges;

	private IloCplex problem;
	private IloIntVar[] conceptVars;
	private IloIntVar[] edgeVars;
	private IloIntVar[] flowVars;

	public SubgraphILP(List<Concept> concepts, List<Proposition> propositions, int maxSize) {
		super(concepts, propositions, maxSize);

		long start = System.currentTimeMillis();
		Logger.getGlobal().log(Level.INFO, "Creating ILP");

		this.conceptIds = new ObjectIntHashMap<Concept>(concepts.size());
		for (int i = 0; i < this.concepts.size(); i++)
			this.conceptIds.put(this.concepts.get(i), i);

		this.relations = this.propositions.stream().map(p -> new CPair(p.sourceConcept, p.targetConcept))
				.collect(Collectors.toSet());

		this.edges = new CPair[this.concepts.size() * this.concepts.size()];
		int id = 0;
		for (Concept c1 : this.concepts) {
			this.edges[id++] = new CPair(null, c1);
			for (Concept c2 : this.concepts) {
				if (c1 != c2) {
					this.edges[id++] = new CPair(c1, c2);
				}
			}
		}

		this.createProblem();

		double duration = (System.currentTimeMillis() - start) / 1000.0;
		Logger.getGlobal().log(Level.INFO, "- done: " + duration);
	}

	@Override
	public Set<Concept> getSubgraph() {

		long start = System.currentTimeMillis();
		Logger.getGlobal().log(Level.INFO, "Solving ILP");

		double[] sol = null;
		try {
			problem.setOut(null);
			boolean solved = problem.solve();
			if (!solved) {
				System.err.println("cplex failed");
			}
			sol = problem.getValues(this.conceptVars);

			if (DEBUG) {
				System.out.println(this.problem);
				System.out.println(this.problem.getObjValue());
				System.out.println(Arrays.stream(this.conceptVars).map(v -> {
					try {
						return v.getName() + "=" + this.problem.getValue(v);
					} catch (Exception e) {
						return "";
					}
				}).collect(Collectors.joining(", ")));
				System.out.println(Arrays.stream(this.edgeVars).map(v -> {
					try {
						return v.getName() + "=" + this.problem.getValue(v);
					} catch (Exception e) {
						return "";
					}
				}).collect(Collectors.joining(", ")));
				System.out.println(Arrays.stream(this.flowVars).map(v -> {
					try {
						return v.getName() + "=" + this.problem.getValue(v);
					} catch (Exception e) {
						return "";
					}
				}).collect(Collectors.joining(", ")));
			}
			System.out.println(this.problem.getObjValue());

		} catch (IloException e) {
			System.err.println("Error solving ILP");
			e.printStackTrace();
			return null;
		}

		Set<Concept> conceptsInSubgraph = new HashSet<Concept>();
		for (int i = 0; i < sol.length; i++) {
			if (sol[i] > 0.99)
				conceptsInSubgraph.add(this.concepts.get(i));
		}

		double duration = (System.currentTimeMillis() - start) / 1000.0;
		Logger.getGlobal().log(Level.INFO, "- done: " + duration);

		return conceptsInSubgraph;
	}

	private void createProblem() {
		try {

			this.problem = new IloCplex();
			this.addObjective();
			this.addSizeConstraint();
			this.addConnectivityConstraints();

			System.out.println("variables: " + this.problem.getNcols() + ", constraints: " + this.problem.getNrows());

		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	private void addObjective() throws IloException {

		// one binary variable per concept
		if (DEBUG) {
			String[] names = new String[this.concepts.size()];
			for (int i = 0; i < names.length; i++)
				names[i] = "c" + i;
			this.conceptVars = this.problem.boolVarArray(this.concepts.size(), names);
		} else {
			this.conceptVars = this.problem.boolVarArray(this.concepts.size());
		}

		// sum of weights of selected concepts
		IloLinearNumExpr obj = this.problem.linearNumExpr();
		for (int i = 0; i < this.conceptVars.length; i++)
			obj.addTerm(this.conceptVars[i], this.concepts.get(i).weight);
		this.problem.addMaximize(obj);

	}

	private void addSizeConstraint() throws IloException {

		// number of selected concepts less than limit
		this.problem.addLe(this.problem.sum(this.conceptVars), this.maxSize);

	}

	private void addConnectivityConstraints() throws IloException {

		// A) edges

		if (DEBUG) {
			String[] names = new String[this.edges.length];
			for (int i = 0; i < this.edges.length; i++) {
				CPair e = this.edges[i];
				String c1 = this.conceptIds.containsKey(e.c1) ? this.conceptIds.get(e.c1) + "" : "x";
				String c2 = this.conceptIds.containsKey(e.c2) ? this.conceptIds.get(e.c2) + "" : "x";
				names[i] = "e" + c1 + c2;
			}
			this.edgeVars = this.problem.boolVarArray(this.edges.length, names);
		} else {
			this.edgeVars = this.problem.boolVarArray(this.edges.length);
		}

		// edge only where relation
		for (int i = 0; i < this.edges.length; i++) {
			if (this.edges[i].c1 != null && !this.relations.contains(this.edges[i]))
				this.problem.addEq(this.edgeVars[i], 0);
		}

		// edge only if concept included
		for (int i = 0; i < this.edges.length; i++) {
			CPair e = this.edges[i];
			// source
			if (e.c1 != null)
				this.problem.addLe(this.edgeVars[i], this.conceptVars[this.conceptIds.get(e.c1)]);
			// target
			this.problem.addLe(this.edgeVars[i], this.conceptVars[this.conceptIds.get(e.c2)]);
		}

		// root node only connects to one concept
		IloLinearIntExpr sum = this.problem.linearIntExpr();
		for (int i = 0; i < this.edges.length; i++) {
			if (this.edges[i].c1 == null)
				sum.addTerm(1, this.edgeVars[i]);
		}
		this.problem.addEq(sum, 1);

		// B) flow

		if (DEBUG) {
			String[] names = new String[this.edges.length];
			for (int i = 0; i < this.edges.length; i++) {
				CPair e = this.edges[i];
				String c1 = this.conceptIds.containsKey(e.c1) ? this.conceptIds.get(e.c1) + "" : "x";
				String c2 = this.conceptIds.containsKey(e.c2) ? this.conceptIds.get(e.c2) + "" : "x";
				names[i] = "f" + c1 + c2;
			}
			this.flowVars = this.problem.intVarArray(this.edges.length, 0, Integer.MAX_VALUE, names);
		} else {
			this.flowVars = this.problem.intVarArray(this.edges.length, 0, Integer.MAX_VALUE);
		}

		// root sends flow <= included concepts
		sum = this.problem.linearIntExpr();
		for (int i = 0; i < this.edges.length; i++) {
			if (this.edges[i].c1 == null) {
				sum.addTerm(1, this.flowVars[i]);
				sum.addTerm(-1, this.conceptVars[this.conceptIds.get(this.edges[i].c2)]);
			}
		}
		this.problem.addEq(sum, 0);

		// concepts consumes one flow
		for (int i = 0; i < this.concepts.size(); i++) {
			Concept c = this.concepts.get(i);
			sum = this.problem.linearIntExpr();
			for (int j = 0; j < this.edges.length; j++) {
				if (this.edges[j].c2 == c)
					sum.addTerm(1, this.flowVars[j]);
				if (this.edges[j].c1 == c)
					sum.addTerm(-1, this.flowVars[j]);
			}
			sum.addTerm(-1, this.conceptVars[this.conceptIds.get(c)]);
			this.problem.addEq(sum, 0);
		}

		// flow only where edges are present
		for (int i = 0; i < this.edges.length; i++) {
			IloLinearIntExpr lin = this.problem.linearIntExpr();
			lin.addTerm(this.concepts.size(), this.edgeVars[i]);
			lin.addTerm(-1, this.flowVars[i]);
			this.problem.addGe(lin, 0);
		}

	}

}
