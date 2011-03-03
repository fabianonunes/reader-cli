package com.fabianonunes.reader.text.classification;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jdom.JDOMException;

import com.fabianonunes.reader.text.analysys.CustomAnalyzer;
import com.fabianonunes.reader.text.classification.rules.ClassifierRule;

public class Classifier {

	private FSDirectory d;
	private IndexSearcher searcher;
	private IndexReader reader;
	private Analyzer analyzer = new CustomAnalyzer();
	private ComplexPhraseQueryParser parser;

	public Classifier(File indexFolder) throws IOException {

		d = FSDirectory.open(indexFolder);

		searcher = new IndexSearcher(d);

		reader = searcher.getIndexReader();

		parser = new ComplexPhraseQueryParser(Version.LUCENE_30, "content",
				analyzer);

	}

	public TreeMap<String, List<Integer>> analyze(File[] rules)
			throws JDOMException, IOException, ParseException {

		TreeMap<String, List<Integer>> results = new TreeMap<String, List<Integer>>();

		for (File file : rules) {

			results.putAll(search(file));

		}

		return results;

	}

	private Map<String, List<Integer>> search(File queryFile)
			throws JDOMException, IOException, ParseException {

		TreeMap<String, List<Integer>> results = new TreeMap<String, List<Integer>>();

		ClassifierRule rule = new ClassifierRule(queryFile);

		String ruleName = rule.getName();

		String textQuery = rule.queryString();

		Query query = parser.parse(textQuery);

		query.rewrite(reader);

		TopDocs topDocs = searcher.search(query, 9999);

		for (ScoreDoc doc : topDocs.scoreDocs) {

			Document document = reader.document(doc.doc);

			Integer pageNumber = Integer.parseInt(document.get("page"));

			if (results.get(ruleName) == null) {

				results.put(ruleName, new ArrayList<Integer>());

			}

			results.get(ruleName).add(pageNumber);

		}

		return results;

	}

	public void close() throws IOException {

		searcher.close();

	}

}
