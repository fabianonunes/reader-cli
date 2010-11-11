package com.fabianonunes.reader.cli;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PageExtractorTest extends AbstractTest {

	PageExtractor extractor;

	@Before
	public void setUp() throws Exception {

		super.setUp();

		extractor = new PageExtractor(pddoc);

	}

	@Test
	public void testGetPage() {

		PDPage page = extractor.getPage(0);

		Assert.assertNotNull(page);

	}

	@Test
	public void testFilterPages() throws IOException, COSVisitorException {

		// FileOutputStream os = new FileOutputStream("/home/fabiano/t.pdf");
		// PDDocument doc = new PDDocument();
		// doc.save(os);
		// for (PDPage page : pages) {
		// doc.importPage(page);
		// }

		List<PDPage> pages = extractor.filterPagesByBookmarkName("^Procura.*");

		Assert.assertEquals(2, pages.size());

	}

}
