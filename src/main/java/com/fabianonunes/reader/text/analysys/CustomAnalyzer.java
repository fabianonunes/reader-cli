package com.fabianonunes.reader.text.analysys;

import java.io.Reader;
import java.util.Arrays;
import java.util.Set;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LengthFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;

public class CustomAnalyzer extends Analyzer {

	public final static String[] STOP_WORDS = { "de", "da", "do", "em", "que",
			"no", "ao", "os", "para", "as", "por", "na", "dos", "se", "nos",
			"ou", "das", "pela", "foi", "sao", "que", "para", "por", "com",
			"dos", "das", "nos" };

	public static Set<?> getDefaultStopSet() {
		return DefaultSetHolder.DEFAULT_STOP_SET;
	}

	private static class DefaultSetHolder {
		static final Set<?> DEFAULT_STOP_SET = CharArraySet
				.unmodifiableSet(new CharArraySet(Arrays.asList(STOP_WORDS),
						false));
	}

	public TokenStream tokenStream(String fieldName, Reader reader) {

		TokenStream stream = new WhitespaceTokenizer(reader);

		stream = new LengthFilter(stream, 3, 25);
		stream = new ImportantFilter(stream);
		if (!fieldName.equals("real")) {
			stream = new LowerCaseFilter(stream);

		}
		stream = new ASCIIFoldingFilter(stream);
		stream = new OCRFilter(stream);
		stream = new LengthFilter(stream, 2, 25);
		stream = new StopFilter(false, stream, getDefaultStopSet());

		return stream;

	}
}