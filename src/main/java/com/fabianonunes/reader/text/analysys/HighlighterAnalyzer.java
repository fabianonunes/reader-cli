package com.fabianonunes.reader.text.analysys;

import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LengthFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;

public class HighlighterAnalyzer extends Analyzer {

	// public TokenStream reusableTokenStream(String fieldName, Reader reader)
	// throws IOException {
	//
	// SavedStreams streams = (SavedStreams) getPreviousTokenStream();
	//
	// if (streams == null) {
	// streams = new SavedStreams();
	// setPreviousTokenStream(streams);
	//
	// streams.tokenizer = new StandardTokenizer(Version.LUCENE_30, reader);
	// streams.stream = new StandardFilter(streams.tokenizer);
	// streams.stream = new LengthFilter(streams.stream, 3, 25);
	// streams.stream = new LowerCaseFilter(streams.stream);
	// streams.stream = new ASCIIFoldingFilter(streams.stream);
	// // streams.stream = new BrazilianStemFilter(streams.stream);
	// } else {
	// streams.tokenizer.reset(reader);
	// }
	//
	// return streams.stream;
	// }
	// private class SavedStreams {
	// Tokenizer tokenizer;
	// TokenStream stream;
	// }

	public TokenStream tokenStream(String fieldName, Reader reader) {

		TokenStream stream = new WhitespaceTokenizer(reader);

		// new StandardTokenizer(Version.LUCENE_30, reader);
		// stream = new StandardFilter(stream);
		// stream = new StopFilter(
		// StopFilter
		// .getEnablePositionIncrementsVersionDefault(Version.LUCENE_30),
		// stream, BrazilianAnalyzer.getDefaultStopSet());
		stream = new LengthFilter(stream, 3, 35);
		if (!fieldName.equals("real")) {
			stream = new LowerCaseFilter(stream);

		}
		stream = new ASCIIFoldingFilter(stream);

		return stream;

	}
}