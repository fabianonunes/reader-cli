package com.fabianonunes.reader.text.analysys;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class OCRFilter extends TokenFilter {

	private char[] output = new char[512];
	private TermAttribute termAtt;
	private OffsetAttribute offsetAtt;

	public OCRFilter(TokenStream in) {
		super(in);
		termAtt = addAttribute(TermAttribute.class);
		offsetAtt = addAttribute(OffsetAttribute.class);
	}

	@Override
	public final boolean incrementToken() throws IOException {

		if (input.incrementToken()) {

			String retVal = "";

			final char[] chars = termAtt.term().toCharArray();
			final int length = termAtt.termLength();
			for (int i = 0; i < length; i++) {

				char c = chars[i];

				String sc = new String(new char[] { c });

				if (Character.isLetterOrDigit(c) || sc.equals(" ")
						|| sc.equals("_")) {

					retVal += sc;

				}

			}

			output = retVal.toCharArray();

			if (!termAtt.term().equals(retVal)) {
				
				termAtt.setTermBuffer(output, 0, retVal.length());
				
				offsetAtt.setOffset(offsetAtt.startOffset(),
						offsetAtt.startOffset() + retVal.length());
				
			}

			return true;

		} else {

			return false;

		}
	}
}
