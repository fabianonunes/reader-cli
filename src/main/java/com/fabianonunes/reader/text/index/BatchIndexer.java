package com.fabianonunes.reader.text.index;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.fabianonunes.reader.pdf.text.position.FullXML;
import com.fabianonunes.reader.text.analysys.CustomAnalyzer;

public class BatchIndexer {

	private IndexWriter indexWriter;
	private Directory d;

	public BatchIndexer(File indexDir) throws IOException {

		d = FSDirectory.open(indexDir);

		indexWriter = new IndexWriter(d, new CustomAnalyzer(),
				MaxFieldLength.UNLIMITED);

	}

	public void index(File file, String docName) throws Exception {

		System.out.println(file);

		FullXML fxml = new FullXML(file);

		Map<Integer, StringBuffer> contents = fxml.getTextOfPages();

		Map<Integer, StringBuffer> head = fxml.getTextOfPages(.35f);

		for (Integer key : contents.keySet()) {

			Document document = new Document();

			document.add(new Field("content", contents.get(key).toString(),
					Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

			if (head.containsKey(key)) {
				document.add(new Field("head", head.get(key).toString(),
						Field.Store.YES, Field.Index.ANALYZED,
						Field.TermVector.YES));
			}

			document.add(new Field("page", Integer.toString(key),
					Field.Store.YES, Field.Index.NOT_ANALYZED));

			document.add(new Field("docName", docName, Field.Store.YES,
					Field.Index.NOT_ANALYZED));

			indexWriter.addDocument(document);

		}

		fxml.close();

	}

	public static void main2(String[] args) throws Exception {

		File dir = new File("/media/TST02/Processos-Analysys/20-40");

		FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
		File[] xmls = dir.listFiles(filter);

		final BatchIndexer bi = new BatchIndexer(new File(
				"/media/TST02/Processos-Analysys/40-100/index"));

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<String>> tasks = new LinkedList<Future<String>>();

		for (final File file : xmls) {

			Future<String> future = executor.submit(new Callable<String>() {

				@Override
				public String call() throws Exception {

					bi.index(file,
							FilenameUtils.getBaseName(file.getAbsolutePath()));
					return null;
				}
			});

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

		bi.close();

		executor.shutdown();

	}

	public void close() throws CorruptIndexException, IOException {

		System.out.println("Closing index...");
		indexWriter.commit();
		indexWriter.optimize();
		indexWriter.close();

	}
}
