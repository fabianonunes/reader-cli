package com.fabianonunes.reader.text.analysys;

import java.io.IOException;
import java.util.Stack;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

public class SynonymFilter extends TokenFilter {
	
	public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";

	private Stack<String> synonymStack;
	
	private SynonymEngine engine;
	
	private AttributeSource.State current;

	private final TermAttribute termAtt;
	private final PositionIncrementAttribute posIncrAtt;

	public SynonymFilter(TokenStream in, SynonymEngine engine) {
		
		super(in);
		
		synonymStack = new Stack<String>(); // #1
		
		this.engine = engine;

		this.termAtt = addAttribute(TermAttribute.class);
		this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
		
	}

	public boolean incrementToken() throws IOException {
		if (synonymStack.size() > 0) { // #2
			String syn = synonymStack.pop(); // #2
			restoreState(current); // #2
			termAtt.setTermBuffer(syn);
			posIncrAtt.setPositionIncrement(0); // #3
			return true;
		}

		if (!input.incrementToken()) // #4
			return false;

		if (addAliasesToStack()) { // #5
			current = captureState(); // #6
		}

		return true; // #7
	}

	private boolean addAliasesToStack() throws IOException {
		String[] synonyms = engine.getSynonyms(termAtt.term()); // #8
		if (synonyms == null) {
			return false;
		}
		for (String synonym : synonyms) { // #9
			synonymStack.push(synonym);
		}
		return true;
	}
}
