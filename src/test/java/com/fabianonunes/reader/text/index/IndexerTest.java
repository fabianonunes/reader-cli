package com.fabianonunes.reader.text.index;

import java.io.File;

import junit.framework.Assert;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexerTest {

	private Indexer indexer;
	private File xmlFile;
	private Directory dir;

	@Before
	public void setUp() throws Exception {

		dir = new RAMDirectory();

		indexer = new Indexer(dir, "test");

		xmlFile = new File(getClass().getResource("/text/opti.xml").toURI());

	}

	@After
	public void tearDown() throws Exception {

		indexer.close();

	}

	@Test
	public void testIndexXMLFile() throws Exception {

		indexer.indexXMLFile(xmlFile);

		indexer.getIndexWriter().commit();

		IndexSearcher s = new IndexSearcher(dir);

		QueryParser p = new QueryParser(Version.LUCENE_40, "word",
				new WhitespaceAnalyzer(Version.LUCENE_40));

		Query q = p.parse("content:correia");

		TopDocs topDocs = s.search(q, 10);

		Assert.assertEquals(1, topDocs.totalHits);

	}

}
