package pipeline;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import model.Concept;
import model.ExtractionResult;
import model.Proposition;

public class ExportGraphs {

	public static final String folderName = "data/CMapSummaries/dummy";
	public static final String name = "concept-graph";

	public static void main(String[] args) throws IOException {

		File folder = new File(folderName);
		for (File clusterFolder : folder.listFiles()) {
			if (clusterFolder.isDirectory()) {

				String serFileName = folderName + "/" + clusterFolder.getName() + "/" + name + ".groups.ser";
				ExtractionResult res = ExtractionResult.load(serFileName);

				CharSequence adjList = toGML(res);

				String graphFileName = folderName + "/" + clusterFolder.getName() + "/" + name + ".graph";
				FileUtils.write(new File(graphFileName), adjList, Charsets.UTF_8);

				System.out.println(clusterFolder.getName() + " " + res.concepts.size() + " " + res.propositions.size());

			}
		}
	}

	private static StringBuilder toGML(ExtractionResult res) {

		StringBuilder sb = new StringBuilder();
		sb.append("graph [\n");
		sb.append("multigraph 1\n");
		sb.append("directed 1\n");

		for (Concept c : res.concepts) {
			sb.append("node [\n");
			sb.append("id ").append(c.id).append("\n");
			sb.append("label ").append(c.id).append("\n");
			sb.append("name \"").append(getCleanLabel(c.name)).append("\"\n");
			sb.append("]\n");
		}

		for (Proposition p : res.propositions) {
			sb.append("edge [\n");
			sb.append("source ").append(p.sourceConcept.id).append("\n");
			sb.append("target ").append(p.targetConcept.id).append("\n");
			sb.append("id ").append(p.id).append("\n");
			sb.append("name \"").append(getCleanLabel(p.relationPhrase)).append("\"\n");
			sb.append("]\n");
		}

		sb.append("]\n");
		return sb;
	}

	private static String getCleanLabel(String label) {
		return label.replaceAll("\\P{Print}", "").replaceAll("\"", "");
	}

}
