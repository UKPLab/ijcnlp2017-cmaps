package grouping.clf.sim;

import java.io.Serializable;

import util.StringPair;

public class CachedSim implements Serializable {

	private static final long serialVersionUID = 1L;

	public StringPair pair;
	public double sim;

	public CachedSim(StringPair sp, double sim) {
		this.pair = sp;
		this.sim = sim;
	}

}
