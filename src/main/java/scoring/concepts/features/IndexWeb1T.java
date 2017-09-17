package scoring.concepts.features;

import java.io.IOException;

import com.googlecode.jweb1t.JWeb1TIndexer;

public class IndexWeb1T {

	public static void main(String[] args) throws IOException {

		JWeb1TIndexer indexer = new JWeb1TIndexer("C:\\Data\\web1t", 3);
		indexer.create();

	}

}
