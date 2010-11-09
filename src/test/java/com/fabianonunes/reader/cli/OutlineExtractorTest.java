package com.fabianonunes.reader.cli;

import java.io.IOException;

import net.sf.json.JSONArray;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OutlineExtractorTest extends AbstractTest {

	OutlineExtractor box;

	@Before
	public void setUp() throws Exception {

		super.setUp();

		box = new OutlineExtractor(pddoc);

	}

	@Test
	public void testCharWidth() throws IOException {

		PDFont font = PDType1Font.HELVETICA_BOLD;

		float textWidth = (font.getStringWidth(" ") / 1000) * 13;

		System.out.println(textWidth);
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
