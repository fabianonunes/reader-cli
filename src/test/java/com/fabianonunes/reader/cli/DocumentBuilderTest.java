package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;

public class DocumentBuilderTest {

	DocumentBuilder builder;

	@Before
	public void setUp() throws Exception {

		builder = new DocumentBuilder(new File(
				"/media/TST02/Processos-Analysys/Pauta 2010-26/"));

	}

	@Test
	public void testIterate() throws InterruptedException, ExecutionException,
			IOException, COSVisitorException {

		PDDocument output = new PDDocument();

		builder.iterate("^Procura.*", new File("/home/fabiano/"), output);

		output.save("/home/fabiano/t.pdf");

	}

}
