
# CMapSummaries Baseline System

Accompanying code for our [IJCNLP 2017 paper](https://www.ukp.tu-darmstadt.de/publications/?no_cache=1&tx_dppublications_pi1%5Bpublication%5D=10714&tx_dppublications_pi1%5Baction%5D=show&tx_dppublications_pi1%5Bcontroller%5D=Publication&cHash=43939e4b0160117841051fd79a03274c#dp_publications-single). Please use the following citation:

```
@inproceedings{ijcnlp17-cmapmds,
	title = {Concept-Map-Based Multi-Document Summarization using Concept Coreference Resolution and Global Importance Optimization},
	author = {Falke, Tobias and Meyer, Christian M. and Gurevych, Iryna},
	booktitle = {Proceedings of the 8th International Joint Conference on Natural Language Processing},
	pages = {(to appear)},
	year = {2017},
	location = {Taipei, Taiwan}
}
```

> **Abstract:** Concept-map-based multi-document summarization is a variant of traditional summarization that produces structured summaries in the form of concept maps. In this work, we propose a new model1 for the task that addresses several issues in previous methods. It learns to identify and merge coreferent concepts to reduce redundancy, determines their importance with a strong supervised model and finds an optimal summary concept map via integer linear programming. It is also computationally more efficient than previous methods, allowing us to summarize larger document sets. We evaluate the model on two datasets, finding that it outperforms several approaches from previous work.

**Contacts** 
  * Tobias Falke, lastname@aihphes.tu-darmstadt.de
  * https://www.ukp.tu-darmstadt.de
  * https://www.aiphes.tu-darmstadt.de

Don't hesitate to send us an e-mail or report an issue, if something is broken (and it shouldn't be) or if you have further questions.

> This repository contains experimental software and is published for the sole purpose of giving additional background details on the respective publication. 

## Requirements

The software is implemented as a Java Maven project. Most dependencies are specified in `pom.xml` and will be automatically downloaded by Maven from central repositories.

Requirements:

* Java (tested with 1.8)
* Maven (tested with 3.3.9)
* Python 2

Non-maven dependencies:

* OpenIE-4: Please clone the repository at https://github.com/knowitall/openie, build it following the instructions given there and make sure the path in `pom.xml` points to the generated jar-file.
* Semilar: Please download the main package and LSA models at http://www.semanticsimilarity.org/. Make sure that Semilar-1.0.jar is in `lib` (or change `pom.xml` accordingly). The code will look for the LSA models in `../semilar/resouces` and WordNet in `../semilar/WordNet-JWI`.
* CPLEX: To solve ILPs, the code uses CPLEX, which can be obtained from IBM here: https://ibm.com/software/commerce/optimization/cplex-optimizer/. According to the `pom.xml`, the project expects the CPLEX Java bindings in `lib/cplex.jar`.


## Data

The system expects the corpus to be available in `data` in the following structure:

* data
	* CMapSummaries
		* train (documents and reference map)
			* topic1
				* topic1.cmap
				* doc1.txt
				* ...
			* topic2
			* ...
		* train_system (maps created by the system)
			* topic1
				* baseline.cmap
			* ...
		* test
			* ...
		* test_system
			* ...

Other datasets should have a similar structure.


## Usage

Before running the system, make sure all requirements are satisfied, dependencies available and the source code has been successfully compiled. 

If the system is run on another dataset as the default one, please adjust the paths accordingly. All necessary paths are defined directly in the classes mentioned below as static variables (no command line arguments added yet, but you can easily do so).

### A) Run the system on input documents

#### 1. Preprocessing

Run `pipeline.PipelinePreprocessing` to run the linguistic preprocessing on all source documents using the DKPro UIMA framework. Results will be stored as binary UIMA Cas serializations in the original document folder (*.txt.bin6-files). 

By default, the program will use the standard models of the different Stanford CoreNLP tools loaded by DKPro. To recreate our exact experimental setting, you can also manually download the CoreNLP models we used and supply them using the arguments of the corresponding UIMA analysis engine. We used the following models:
* PoS-Tagging: english-left3words-distsim from 3.6.0
* Named Entities: english.all.3class.distsim.crf.ser.gz from 3.6.0
* Parser: englishRNN.ser.gz from 3.6.0
* Coreference: dcoref/* from 3.6.0

Please note that we call the tokenization module with the option PARAM_NEWLINE_IS_SENTENCE_BREAK="ALWAYS", which only makes sense if you can be sure that sentences will never cross line breaks.

#### 2. Open Information Extraction

Run `pipeline.PipelineOpenIE` to run the Open Information Extraction system on the input documents. Results will be stored as binary UIMA Cas serializations in the original document folder (*.oie.bin6-files). 

#### 3. Concept Graph Construction

Run `pipeline.PipelineGraph` on the created oie-files to construct the concept graph. This program will extract concepts and relations from the OpenIE output, then find coreferences between concepts and create a corresponding graph. Results will be stored in a Java serialization file (*.groups.ser).

This step relies on several resources:
* Word2Vec Embeddings: Download the 300d Google News word2vec embeddings available at https://drive.google.com/file/d/0B7XkCwpI5KDYNlNUTTlSS21pQmM/edit?usp=sharing and make sure the path in `grouping.clf.sim.WordEmbeddingDistance` points to the archive.
* If the program cannot access WordNet, you might need to create a symbolic link from `<project-folder>/WordNet-JWI` to its actual location, e.g. in the semilar folder.

You need to make sure that the program has enough memory, as the computation of similarities between all extracted concepts can result a high number of comparisons, depending on the size and number of input documents. We usually run this part as follows:

```
export MAVEN_OPTS=-Xmx100g -Djava.library.path=""
mvn exec:java -Dexec.mainClass=pipeline.PipelineGraph -Dexec.classpathScope=compile
```

This program uses a trained model that weights the (five) features of the coreference classifier. In `models`, we provide our trained models for the two datasets used in the paper. The path to the model needs to be defined in `grouping.ConceptGrouperSimLog`.

#### 4. Graph Summarization

In the final step, we extract a summary concept map from the graph constructed in the previous step. In order to do that, the program will extract features for all concepts, score them for importance and select a subgraph as the final summary.

This step relies on the following resources:
- MRC: A dictionary of different word categories that we derived from the [MRC Psycholinguistic Database](http://websites.psychology.uwa.edu.au/school/MRCDatabase/uwa_mrc.htm). It as already available in `src/main/resources/dict/MRC.tsv`.
- [LIWC](https://liwc.wpengine.com/): Another dictionary of word categories. The file `src/main/resources/dict/LIWC.names` lists the categories we use. A second file, `LIWC.csv`, is required that maps words to these categories. A line should contain the word and all categories it belongs to, separated by commas.
- Concreteness Values: A list of concreteness values published by [Brysbaert et al. 2013](http://crr.ugent.be/archives/1330). We provide it in the correct data format in `src/main/resources/dict/Brysbaert.tsv`, licensed under CC BY-NC-ND 3.0 as the original dataset.
- Web1T ngram counts: We use [jweb1t](https://github.com/dkpro/jweb1t) to access the data. Please obtain the data files, run `scoring.concepts.features.IndexWeb1T` to create the index and then set the corresponding path in `util.FrequencyProvider`.

#### 4.1 Export Graphs to GML

Run `pipeline.ExportGraphs` to create GML files for the graphs created in the previous step. The GML markup of the graphs will be stored in *.graph-files.

#### 4.2 Compute Graph Features in Python

Run the python script `src/main/scripts/scoring/compute_graph_features.py` to compute features that are based on the graph structure. The script requires the python package [networkx](https://networkx.github.io/). The computed features will be stored in *.graph_features.tsv-files.

#### 4.3 Compute remaining Features

Run the following program to extract additional features and persist all of them in a WEKA arff-file (you might need to change the paths in `ComputeFeatures` before:

```
export MAVEN_OPTS=-Xmx10g -Djava.library.path=""
mvn exec:java -Dexec.mainClass=pipeline.ComputeFeatures -Dexec.classpathScope=compile
```

#### 4.4 Score Concepts and Create Summary

Finally, run the pipeline `pipeline.PipelineSummary`, which scores all concepts based on the extracted features and the selects a summary concept maps. This program requires CPLEX to be available. It will read the graph form the *.groups.ser-file, all features from the *.arff-file and produce the summary concept map as a *.cmap file the document cluster's folder. The content are tab-separated propositions of the concept map.

We run this step as follows:
```
export MAVEN_OPTS="-Djava.library.path=/path-to-cplex/CPLEX_Studio127/cplex/bin/x86-64_linux"
mvn exec:java -Dexec.mainClass=pipeline.PipelineSummary -Dexec.classpathScope=compile
```

This program uses a trained scoring model. In `models`, we provide our trained models for the two datasets used in the paper. The path to the model needs to be defined in `scoring.concepts.ConceptScorerRanking`. There, you also need to specify the path to the arff-file containing the correct features for the trained model.


### B) Evaluate generated concept maps

To compare generated map against reference concept maps, please refer to the instructions given here: https://github.com/UKPLab/emnlp2017-cmapsum-corpus/blob/master/eval/README.md


