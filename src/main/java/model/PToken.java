package model;

import java.io.Serializable;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class PToken implements Serializable {

	private static final long serialVersionUID = 1L;

	public String documentId;
	public String sentId;
	public int tokenId;
	public int start;
	public int end;
	public int docLength;

	public String text;
	public String pos;
	public String lemma;
	public String neTag;

	public PToken(String text) {
		this.text = text;
	}

	public PToken(Token t) {
		this.text = t.getCoveredText();
		this.pos = t.getPos() != null ? t.getPos().getPosValue() : null;
		this.lemma = t.getLemma() != null ? t.getLemma().getValue() : null;
		List<NamedEntity> nes = JCasUtil.selectCovered(NamedEntity.class, t);
		if (nes.size() > 0)
			this.neTag = nes.get(0).getValue();

		DocumentMetaData meta = (DocumentMetaData) t.getCAS().getDocumentAnnotation();
		this.documentId = meta.getDocumentId();
		this.start = t.getBegin();
		this.end = t.getEnd();
		this.docLength = t.getCAS().getDocumentText().length();
	}

	public PToken(Token t, String sentKey, int tokenId) {
		this(t);
		this.sentId = sentKey;
		this.tokenId = tokenId;
	}

	@Override
	public String toString() {
		return text + "(" + pos + "/" + lemma + "/" + neTag + ")";
	}

}
