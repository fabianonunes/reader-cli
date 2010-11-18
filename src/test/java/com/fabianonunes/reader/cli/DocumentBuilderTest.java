package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.junit.Before;
import org.junit.Test;

public class DocumentBuilderTest {

	DocumentBuilder builder;

	@Before
	public void setUp() throws Exception {

		builder = new DocumentBuilder(new File(
				"/media/TST02/Processos-Analysys/2010-32"));

	}

	@Test
	public void testIterate() throws InterruptedException, ExecutionException,
			IOException, COSVisitorException {

		builder.iterate("^A..o Rescis.ria.*", new File("/home/fabiano/"));

	}

}
