package com.fabianonunes.reader.text.analysys;

import java.io.IOException;

public interface SynonymEngine {
	String[] getSynonyms(String s) throws IOException;
}
