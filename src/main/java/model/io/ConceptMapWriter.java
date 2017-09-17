package model.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import model.ConceptMap;
import model.Proposition;

/**
 * Writes a concept map to a text file
 */
public class ConceptMapWriter {

	public static boolean writeToFile(ConceptMap map, File file, Format format) {
		switch (format) {
		case TSV:
			return toTsv(map, file);
		default:
			return false;
		}
	}

	private static boolean toTsv(ConceptMap map, File file) {
		StringBuilder res = new StringBuilder();
		List<Proposition> props = new ArrayList<Proposition>(map.getProps());
		Collections.sort(props, new Comparator<Proposition>() {
			@Override
			public int compare(Proposition o1, Proposition o2) {
				return o1.sourceConcept.name.compareTo(o2.sourceConcept.name);
			}
		});
		for (Proposition p : props) {
			String l = p.sourceConcept.name.replaceAll("[\r\n]", " ");
			l += "\t" + p.relationPhrase.replaceAll("[\r\n]", " ");
			l += "\t" + p.targetConcept.name.replaceAll("[\r\n]", " ");
			res.append(l + "\n");
		}
		return write(res.toString(), file);
	}

	private static boolean write(String content, File file) {
		try {
			FileUtils.write(file, content, Charsets.UTF_8);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

}
