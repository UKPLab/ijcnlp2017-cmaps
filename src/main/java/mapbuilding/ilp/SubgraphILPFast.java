package mapbuilding.ilp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import model.CPair;
import model.Concept;
import model.Proposition;

/**
 * 
 * Objective Function:
 * \sum c_i*s_i + \sum
 * 
 * Parameter:
 * L - max number of concepts
 * s_i - score for concept i
 * r_ij - existing relation from i to j
 * 
 * Variables:
 * c_i - concept included - binary
 * g_i - concept not included - binary
 * e_0i - edge from o to i - binary
 * f_ij - flow from i to j - int, >= 0
 * 
 * Constraints:
 * 1) \forall i: \sum c_i <= L (size)
 * 2) \forall ij: f_ij <= c_i*C (flow only if source included)
 * 3) \forall ij: f_ij <= c_j*C (flow only if target included)
 * 4) \sum e_0i = 1 (root link sends flow to only one concept)
 * 5) \forall ij: C*e_0j - f_0j >= 0 (root flow only to the selected concept)
 * 6) \sum f_0i - \sum c_i = 0 (root sends flow <= included concepts)
 * 7) \forall k: \sum f_ik - \sum f_kp - c_k = 0 (concepts consumes on flow)
 * 
 * @author falke
 *
 */
public class SubgraphILPFast extends GraphSummarizer {

	private static final boolean DEBUG = false;

	private int maxTime = 300;
	private ObjectIntMap<Concept> conceptIds;
	private List<CPair> edges;

	private Set<Concept> goldConcepts;
	private boolean withPenalty;
	private double penalty;

	private IloCplex problem;
	private IloIntVar[] conceptVars;
	private IloIntVar[] edgeVars; // root only
	private IloIntVar[] flowVars;

	public SubgraphILPFast(List<Concept> concepts, List<Proposition> propositions, int maxSize) {
		this(concepts, propositions, maxSize, -1, null, 0);
	}

	public SubgraphILPFast(List<Concept> concepts, List<Proposition> propositions, int maxSize, int maxTime) {
		this(concepts, propositions, maxSize, maxTime, null, 0);
	}

	public SubgraphILPFast(List<Concept> concepts, List<Proposition> propositions, int maxSize, int maxTime,
			Set<Concept> goldConcepts, double penalty) {
		super(concepts, propositions, maxSize);

		if (maxTime > 0)
			this.maxTime = maxTime;
		this.goldConcepts = goldConcepts;
		this.penalty = penalty;
		if (goldConcepts != null)
			this.withPenalty = true;

		long start = System.currentTimeMillis();
		Logger.getGlobal().log(Level.INFO, "Creating ILP");

		this.conceptIds = new ObjectIntHashMap<Concept>(concepts.size());
		for (int i = 0; i < this.concepts.size(); i++)
			this.conceptIds.put(this.concepts.get(i), i);

		Set<CPair> edges = new HashSet<CPair>();
		for (Proposition p : this.propositions) {
			edges.add(new CPair(p.sourceConcept, p.targetConcept));
			edges.add(new CPair(p.targetConcept, p.sourceConcept));
		}
		this.edges = new ArrayList<CPair>(edges);

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
			problem.setParam(IloCplex.DoubleParam.TimeLimit, maxTime);
			// problem.tuneParam(); -> takes very long
			boolean solved = problem.solve();
			if (!solved) {
				System.err.println("cplex failed");
			}
			Status status = problem.getStatus();
			if (status.equals(Status.Feasible))
				Logger.getGlobal().log(Level.INFO, "timeout, using best solution");
			else if (!status.equals(Status.Optimal))
				System.err.println("cplex failed");

			sol = problem.getValues(this.conceptVars);

			if (DEBUG) {
				System.out.println(this.problem);
			}

		} catch (IloException e) {
			System.err.println("Error solving ILP");
			e.printStackTrace();
			return null;
		}

		Set<Concept> conceptsInSubgraph = new HashSet<Concept>();
		for (int i = 0; i < this.concepts.size(); i++) {
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

			Logger.getGlobal().log(Level.INFO,
					"variables: " + this.problem.getNcols() + ", constraints: " + this.problem.getNrows());

		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	private void addObjective() throws IloException {

		// one binary variable per concept
		if (DEBUG) {
			int nb = withPenalty ? this.concepts.size() * 2 : this.concepts.size();
			String[] names = new String[nb];
			for (int i = 0; i < this.concepts.size(); i++)
				names[i] = "c" + i;
			if (withPenalty) {
				for (int i = this.concepts.size(); i < this.concepts.size() * 2; i++)
					names[i] = "pc" + i;
			}
			this.conceptVars = this.problem.boolVarArray(nb, names);
		} else {
			int nb = withPenalty ? this.concepts.size() * 2 : this.concepts.size();
			this.conceptVars = this.problem.boolVarArray(nb);
		}

		// with penalty -> contraints for aux variables
		if (withPenalty) {
			for (int i = 0; i < this.concepts.size(); i++) {
				IloLinearIntExpr lin = this.problem.linearIntExpr();
				lin.addTerm(1, this.conceptVars[i]);
				lin.addTerm(1, this.conceptVars[i + this.concepts.size()]);
				this.problem.addEq(1, lin);
			}
		}

		// sum of weights of selected concepts
		IloLinearNumExpr obj = this.problem.linearNumExpr();
		for (int i = 0; i < this.concepts.size(); i++) {
			Concept c = this.concepts.get(i);
			double s = c.weight;
			if (withPenalty && !this.goldConcepts.contains(c))
				s += penalty;
			obj.addTerm(this.conceptVars[i], s);
			if (withPenalty && this.goldConcepts.contains(c))
				obj.addTerm(this.conceptVars[i + this.concepts.size()], penalty);
		}
		this.problem.addMaximize(obj);

	}

	private void addSizeConstraint() throws IloException {

		// number of selected concepts less than limit
		IloLinearIntExpr count = this.problem.linearIntExpr();
		for (int i = 0; i < this.concepts.size(); i++)
			count.addTerm(1, this.conceptVars[i]);
		this.problem.addLe(count, this.maxSize);

	}

	private void addConnectivityConstraints() throws IloException {

		// A) edges (root to concept)

		if (DEBUG) {
			String[] names = new String[this.concepts.size()];
			for (int i = 0; i < names.length; i++)
				names[i] = "eX" + i;
			this.edgeVars = this.problem.boolVarArray(this.concepts.size(), names);
		} else {
			this.edgeVars = this.problem.boolVarArray(this.concepts.size());
		}

		// root node only connects to one concept
		IloLinearIntExpr sumRoot = this.problem.linearIntExpr();
		for (int i = 0; i < this.edgeVars.length; i++)
			sumRoot.addTerm(1, this.edgeVars[i]);
		this.problem.addEq(sumRoot, 1);

		// B) flow (root edges + over edges)

		if (DEBUG) {
			String[] names = new String[this.concepts.size() + this.edges.size()];
			for (int i = 0; i < this.concepts.size(); i++)
				names[i] = "fX" + i;
			for (int i = 0; i < this.edges.size(); i++) {
				CPair e = this.edges.get(i);
				names[this.concepts.size() + i] = "f" + this.conceptIds.get(e.c1) + this.conceptIds.get(e.c2);
			}
			this.flowVars = this.problem.intVarArray(this.concepts.size() + this.edges.size(), 0, Integer.MAX_VALUE,
					names);
		} else {
			this.flowVars = this.problem.intVarArray(this.concepts.size() + this.edges.size(), 0, Integer.MAX_VALUE);
		}

		// root sends flow <= included concepts
		IloLinearIntExpr sum = this.problem.linearIntExpr();
		for (int i = 0; i < this.concepts.size(); i++) {
			sum.addTerm(1, this.flowVars[i]);
			sum.addTerm(-1, this.conceptVars[i]);
		}
		this.problem.addEq(sum, 0);

		// root flow only over root edges
		for (int i = 0; i < this.edgeVars.length; i++) {
			IloLinearIntExpr lin = this.problem.linearIntExpr();
			lin.addTerm(this.concepts.size(), this.edgeVars[i]);
			this.problem.addLe(this.flowVars[i], lin);
		}

		// flow only if concept included
		for (int i = 0; i < this.concepts.size(); i++) {
			IloLinearIntExpr bound = this.problem.linearIntExpr();
			bound.addTerm(this.concepts.size(), this.conceptVars[i]);
			this.problem.addLe(this.flowVars[i], bound);
		}
		for (int i = 0; i < this.edges.size(); i++) {
			CPair e = this.edges.get(i);
			IloLinearIntExpr bound = this.problem.linearIntExpr();
			bound.addTerm(this.concepts.size(), this.conceptVars[this.conceptIds.get(e.c1)]);
			this.problem.addLe(this.flowVars[this.concepts.size() + i], bound);
			bound = this.problem.linearIntExpr();
			bound.addTerm(this.concepts.size(), this.conceptVars[this.conceptIds.get(e.c2)]);
			this.problem.addLe(this.flowVars[this.concepts.size() + i], bound);
		}

		// concepts consumes one flow
		for (int i = 0; i < this.concepts.size(); i++) {
			Concept c = this.concepts.get(i);
			sum = this.problem.linearIntExpr();
			for (int j = 0; j < this.edges.size(); j++) {
				if (this.edges.get(j).c2 == c)
					sum.addTerm(1, this.flowVars[this.concepts.size() + j]);
				if (this.edges.get(j).c1 == c)
					sum.addTerm(-1, this.flowVars[this.concepts.size() + j]);
			}
			sum.addTerm(1, this.flowVars[i]);
			sum.addTerm(-1, this.conceptVars[this.conceptIds.get(c)]);
			this.problem.addEq(sum, 0);
		}

	}

}
