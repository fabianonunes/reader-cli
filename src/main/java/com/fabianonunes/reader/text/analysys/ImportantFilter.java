package com.fabianonunes.reader.text.analysys;

import java.io.IOException;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

public class ImportantFilter extends TokenFilter {

	public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";

	private Stack<String> synonymStack;
	private AttributeSource.State current;

	private final TermAttribute termAtt;
	private final PositionIncrementAttribute posIncrAtt;

	public ImportantFilter(TokenStream in) {

		super(in);

		synonymStack = new Stack<String>();

		this.termAtt = addAttribute(TermAttribute.class);
		this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	}

	public boolean incrementToken() throws IOException {

		if (synonymStack.size() > 0) {
			String syn = synonymStack.pop();
			restoreState(current);
			termAtt.setTermBuffer(syn);
			posIncrAtt.setPositionIncrement(0);
			return true;
		}

		if (!input.incrementToken())
			return false;

		if (addAliasesToStack()) {
			current = captureState();
		}

		return true;

	}

	private boolean addAliasesToStack() throws IOException {

		String[] synonyms = getImportant(termAtt.term());

		if (synonyms == null) {
			return false;
		}

		for (String synonym : synonyms) {
			synonymStack.push(synonym);
		}

		return true;

	}

	private String[] getImportant(String term) {

		term = cleanString(term);

		if (term.length() < 3) {
			return null;
		}

		if (StringUtils.isAllUpperCase(term)) {

			return new String[] { "_" + term + "_" };

		}

		return null;
	}

	public static Boolean isCapitalized(String str) {

		if (str == null || StringUtils.isEmpty(str)) {
			return false;
		}

		if (str.length() < 3) {
			return false;
		}

		if (Character.isUpperCase(str.charAt(0)) == false) {
			return false;
		}

		int sz = str.length();

		for (int i = 1; i < sz; i++) {
			if (Character.isUpperCase(str.charAt(i)) == true) {
				return false;
			}
		}
		return true;

	}

	private static String cleanString(String string) {

		char[] chars = string.toCharArray();

		String retVal = "";

		for (char c : chars) {

			String sc = new String(new char[] { c });

			if (Character.isLetterOrDigit(c)) {

				retVal += sc;

			}

		}

		return retVal;

	}
}
