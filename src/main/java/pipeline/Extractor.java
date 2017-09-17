package pipeline;

import java.util.List;

import model.Concept;
import model.Proposition;

/**
 * Pipeline components implementing this interface offer extracted concepts and
 * relations
 */
public interface Extractor {

	public List<Concept> getConcepts();

	public List<Proposition> getPropositions();

}
