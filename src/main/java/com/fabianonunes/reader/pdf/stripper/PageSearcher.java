package com.fabianonunes.reader.pdf.stripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.fabianonunes.reader.text.analysys.CustomAnalyzer;

public class PageSearcher {

	private IndexSearcher searcher;
	private Analyzer analyzer = new CustomAnalyzer();
	private Directory d;

	public PageSearcher(File indexDir) throws IOException {

		d = FSDirectory.open(indexDir);

		searcher = new IndexSearcher(d, true);

	}

	public Map<String, Set<Integer>> search() throws Exception {

		Map<String, Set<Integer>> results = new TreeMap<String, Set<Integer>>();

		QueryParser p = new ComplexPhraseQueryParser(Version.LUCENE_40, "word",
				analyzer);

		Query query = p.parse("contrarrazoes OR contrarazoes OR contrarrazoar");

		TopDocs topDocs = searcher.search(query, 99999);

		ScoreDoc[] docs = topDocs.scoreDocs;

		System.out.println(topDocs.totalHits);

		for (ScoreDoc doc : docs) {

			Document document = searcher.doc(doc.doc);

			String docName = document.get("docName");

			if (!results.containsKey(docName)) {
				results.put(docName, new TreeSet<Integer>());
			}

			int page = Integer.parseInt(document.get("page"));

			results.get(docName).add(page);

		}

		return results;

	}

	private class ExtractorClosure implements Callable<String> {

		private File pdfFile;
		private File outputFile;
		private List<Integer> pageList;

		public ExtractorClosure(File pdfFile, File outputFile,
				List<Integer> pageList) {

			this.pdfFile = pdfFile;
			this.outputFile = outputFile;
			this.pageList = pageList;

		}

		@Override
		public String call() throws Exception {

			System.out.println(pdfFile);

			PageStripper stripper = new PageStripper(pdfFile, outputFile);

			stripper.extractPages(pageList);

			return null;

		}
	}

	public static void main2(String[] args) throws Exception {

		File baseDir = new File("/media/TST02/Processos-Analysys/20-40");

		final PageSearcher pSearcher = new PageSearcher(new File(baseDir,
				"index"));

		Map<String, Set<Integer>> results = pSearcher.search();

		Set<String> fileNames = results.keySet();

		ExecutorService executor = Executors.newFixedThreadPool(8);

		LinkedList<Future<String>> tasks = new LinkedList<Future<String>>();

		for (String fileName : fileNames) {

			File pdfFile = new File(baseDir, fileName + ".pdf");

			List<Integer> ignoreList;

			try {
				ignoreList = getPagesBookmarked(pdfFile, "^Contra[r-]raz.es.*");
			} catch (Exception e) {
				// quietly
				ignoreList = new ArrayList<Integer>();
			}

			File outputFile = new File(baseDir, fileName + "_s.pdf");

			FileUtils.deleteQuietly(outputFile);

			Set<Integer> pages = results.get(fileName);

			List<Integer> pageList = new ArrayList<Integer>(pages);

			pageList.removeAll(ignoreList);

			Future<String> future = executor
					.submit(pSearcher.new ExtractorClosure(pdfFile, outputFile,
							pageList));

			tasks.add(future);

		}

		for (Future<String> future : tasks) {

			try {

				future.get();

			} catch (Exception e) {

				System.out.println("Error in: " + e.getMessage());
				e.printStackTrace();

			}

		}

		executor.shutdown();

	}

	private static List<Integer> getPagesBookmarked(File pdfFile, String pattern)
			throws IOException {

		PDDocument doc = null;

		try {

			doc = PDDocument.load(pdfFile);

			PageExtractor extractor = new PageExtractor(doc);

			return extractor.filterPageNumbersByBookmarkName(pattern);

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			if (doc != null) {

				doc.close();

			}

		}

		return new ArrayList<Integer>();

	}

}
