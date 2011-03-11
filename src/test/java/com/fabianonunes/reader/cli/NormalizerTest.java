package com.fabianonunes.reader.cli;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.Test;

public class NormalizerTest {
	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	@Test
	public void testNormalize() {
		
		String nowhitespace = WHITESPACE.matcher("- 25100-51.2009.5.24.0000").replaceAll("-");
		
		String normalized = Normalizer.normalize(nowhitespace, Form.NFD);
		
		String slug = NONLATIN.matcher(normalized).replaceAll("");
		
		System.out.println(slug.toLowerCase(Locale.ENGLISH));
		
	}

}
