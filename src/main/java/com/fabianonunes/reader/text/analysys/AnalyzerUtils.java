package com.fabianonunes.reader.text.analysys;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class AnalyzerUtils {

	public static void displayTokens(Analyzer analyzer, String text)
			throws IOException {
		displayTokens(analyzer.tokenStream("content", new StringReader(text)));
	}

	public static void displayTokens(TokenStream stream) throws IOException {

		TermAttribute term = stream.addAttribute(TermAttribute.class);

		OffsetAttribute offset = stream.addAttribute(OffsetAttribute.class);

		PositionIncrementAttribute posAttr = stream
				.addAttribute(PositionIncrementAttribute.class);

		// PayloadAttribute payloadAtt = stream
		// .addAttribute(PayloadAttribute.class);

		int position = 0;
		while (stream.incrementToken()) {

			position += posAttr.getPositionIncrement();

			System.out.print(position + ": ");
			System.out.println("[" + term.term() + "| - |"
					+ offset.startOffset() + "] ");
			// System.out.println(new
			// String(payloadAtt.getPayload().getData()));

		}

	}

}
