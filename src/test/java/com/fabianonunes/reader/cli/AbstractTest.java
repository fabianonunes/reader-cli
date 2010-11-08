package com.fabianonunes.reader.cli;

import java.net.URL;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.After;

public abstract class AbstractTest {

	protected PDDocument pddoc;

	public void setUp() throws Exception {

		URL url = this.getClass().getResource(
				"/pdf-documents/466-55.2010.5.06.0000.pdf");

		pddoc = PDDocument.load(url);

	}

	@After
	public void tearDown() throws Exception {
		pddoc.close();
	}

}
