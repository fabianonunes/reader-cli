package com.fabianonunes.reader.cli;

import java.io.IOException;

import net.sf.json.JSONArray;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OutlineExtractorTest extends AbstractTest {

	OutlineExtractor box;

	@Before
	public void setUp() throws Exception {

		super.setUp();

		// pddoc = PDDocument.load(new File("/media/TST02/Processos-Analysys"
		// + "/Pauta 2010-26/chunk1" + "/4800-25.2009.5.22.0000.pdf"));

		box = new OutlineExtractor(pddoc);

	}

	@After
	public void tearDown() throws Exception {

		box.close();

	}

	@Test
	public void testExtractOutline() throws IOException {

		try {

			JSONArray plain = box.getAllBookmarks();

			Assert.assertEquals(15, plain.size());

		} finally {

			box.close();

		}

	}

}
