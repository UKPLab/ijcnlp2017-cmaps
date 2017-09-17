package grouping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.Concept;
import model.ConceptGroup;
import model.PToken;

/**
 * groups concepts that are the same according to label
 * 
 * criteria: same lemmatized label ignoring all tokens that are not nouns,
 * verbs, adjectives or adverbs
 * 
 * @author falke
 *
 */
public class LemmaGrouper {

	public static Map<Concept, ConceptGroup> group(List<Concept> concepts) {

		// build mapping: label -> group
		Map<String, ConceptGroup> groups = new HashMap<String, ConceptGroup>();
		for (Concept c : concepts) {
			String key = getConceptKey(c);
			if (!groups.containsKey(key))
				groups.put(key, new ConceptGroup());
			groups.get(key).add(c);
		}

		// build mapping: rep -> group
		Map<Concept, ConceptGroup> groupMap = new HashMap<Concept, ConceptGroup>();
		for (Entry<String, ConceptGroup> e : groups.entrySet()) {
			groupMap.put(e.getValue().getRep(), e.getValue());
			e.getValue().getRep().weight = e.getValue().size();
		}

		return groupMap;
	}

	private static String getConceptKey(Concept c) {
		StringBuilder sb = new StringBuilder();
		for (PToken t : c.tokenList) {
			if (t.pos.startsWith("N") || t.pos.startsWith("V") || t.pos.startsWith("J") || t.pos.startsWith("R")) {
				sb.append(t.lemma.toLowerCase());
				sb.append(" ");
			}
		}
		return sb.toString();
	}

}
