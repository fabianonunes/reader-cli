package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.xml.sax.SAXException;

import com.fabianonunes.reader.storage.ReaderDocument;
import com.fabianonunes.reader.tasks.CommandLineExecutor;
import com.fabianonunes.reader.tasks.PdfToImageTask;
import com.fabianonunes.reader.tasks.PdfToXMLTask;
import com.fabianonunes.reader.tasks.XmlAssembler;
import com.fabianonunes.reader.text.index.Indexer;
import com.fabianonunes.reader.text.index.SolrIndexer;
import com.fabianonunes.reader.text.position.OptiXML;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.ModifyException;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.TranscodeException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class Converter {

	public static FileFilter xmlFilter = FileFilterUtils
			.suffixFileFilter(".xml");

	public static FileFilter pgmFilter = FileFilterUtils
			.suffixFileFilter(".pgm");

	public static FileFilter pngFilter = FileFilterUtils
			.suffixFileFilter(".png");

	public static FileFilter pdfFilter = FileFilterUtils
			.suffixFileFilter(".pdf");

	private Mongo m;
	private DB db;
	private GridFS gfs;

	private DBCollection processos;

	private Integer numOfPages;

	private ReaderDocument document;

	public static void main(String[] args) throws Throwable {

		File inputDir = new File("/media/TST02/Processos/Convert");

		inputDir = new File("/home/fabiano/workdir/converter");

		File[] pdfFiles = inputDir.listFiles(pdfFilter);

		for (File file : pdfFiles) {

			ReaderDocument rdd = ReaderDocument.generateDocument(file);

			new Converter(rdd);

			// tc.convert();

		}

	}

	public Converter(ReaderDocument document) throws MongoException,
			IOException {

		this.document = document;

		File pdfFile = document.getPdf();

		numOfPages = calcNumOfPages(pdfFile);

	}

	public void optimizeImages() throws InterruptedException {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		File[] images = document.getImageFolder().listFiles(pngFilter);
		File[] thumbs = document.getThumbsFolder().listFiles(pngFilter);

		ArrayList<File> files = new ArrayList<File>();
		files.addAll(Arrays.asList(images));
		files.addAll(Arrays.asList(thumbs));

		for (final File file : files) {

			Future<Integer> task2 = executor.submit(new Callable<Integer>() {

				@Override
				public Integer call() throws Exception {
					String command = "optipng -q " + file.getAbsolutePath();
					CommandLineExecutor.exec(command);
					return null;
				}
			});

			tasks.add(task2);

		}

		for (Future<Integer> future : tasks) {

			try {
				future.get();
			} catch (Exception e) {
				System.out.println("Error in: " + e.getMessage());
				e.printStackTrace();
			}

		}

		executor.shutdown();

		executor.awaitTermination(20, TimeUnit.MINUTES);

	}

	public void extractText() throws InterruptedException {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		Integer iterations = new Double(Math.ceil(numOfPages / 8f)).intValue();

		Integer step = 8;
		for (int i = 0; i < iterations; i++) {

			PdfToXMLTask xmlTask = new PdfToXMLTask(document);
			xmlTask.setFirstPage(step * i + 1);
			xmlTask.setTotalPages(step - 1);
			xmlTask.setLastPage(numOfPages);

			Future<Integer> task2 = executor.submit(xmlTask);
			tasks.add(task2);

		}

		for (Future<Integer> future : tasks) {

			try {
				future.get();
			} catch (Exception e) {
				System.out.println("Error in: " + e.getMessage());
				e.printStackTrace();
			}

		}

		executor.shutdown();

		executor.awaitTermination(20, TimeUnit.MINUTES);

	}

	public void extractPdfData() {
		try {
			document.extractData();
		} catch (Exception e) {
			// queitly
		}
	}

	public void indexDocument() throws IOException, EncodingException,
			EOFException, EntityException, ParseException, XPathParseException,
			NavException, XPathEvalException, TranscodeException {

		Indexer indexer = new Indexer(document.getIndexFolder(), document
				.getFolder().getName());

		indexer.indexXMLFile(document.getOptiText());

	}

	public void manipulateTextFiles() throws EncodingException, EOFException,
			EntityException, XPathParseException, NavException,
			XPathEvalException, ParseException, IOException, ModifyException,
			TranscodeException {

		File[] files = document.getTextFolder().listFiles(xmlFilter);

		XmlAssembler.assemble(files, document.getFullText(), "//PAGE");

		File fullFile = document.getFullText();
		OptiXML opti = new OptiXML(fullFile);
		opti.optimize();

	}

	public void extractImages() throws InterruptedException {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		Integer iterations = new Double(Math.ceil(numOfPages / 8f)).intValue();

		Integer step = 8;
		for (int i = 0; i < iterations; i++) {

			PdfToImageTask pdfTask = new PdfToImageTask(document);
			pdfTask.setFirstPage(step * i + 1);
			pdfTask.setTotalPages(step - 1);
			pdfTask.setLastPage(numOfPages);

			Future<Integer> task = executor.submit(pdfTask);
			tasks.add(task);

		}

		for (Future<Integer> future : tasks) {

			try {
				future.get();
			} catch (Exception e) {
				System.out.println("Error in: " + e.getMessage());
				e.printStackTrace();
			}

		}

		executor.shutdown();

		executor.awaitTermination(20, TimeUnit.MINUTES);

	}

	public void storeToDB() throws IOException {

		String name = document.getFolder().getName();

		m = new Mongo();

		db = m.getDB("sesdi2");

		processos = db.getCollection("processos");

		BasicDBObject qname = new BasicDBObject("name", name);

		processos.remove(qname);

		processos.ensureIndex(new BasicDBObject("name", 1).append("unique",
				true));

		gfs = new GridFS(db, "images");
		gfs.remove(qname);

		BasicDBObject doc = new BasicDBObject();
		doc.append("name", name);
		doc.append("pages", numOfPages);
		processos.save(doc);

		File[] thumbsFiles = document.getThumbsFolder().listFiles(pngFilter);
		File[] pngFiles = document.getImageFolder().listFiles(pngFilter);
		File[] xmlGzFiles = document.getSimpleTextFolder().listFiles();

		storeImages(pngFiles, name, name);
		storeImages(thumbsFiles, name + "/t", name);
		storeImages(xmlGzFiles, name + "/s", name);

	}

	private Integer calcNumOfPages(File pdfFile) throws IOException {

		RandomAccessFileOrArray raf = new RandomAccessFileOrArray(
				pdfFile.getAbsolutePath());

		PdfReader reader = new PdfReader(raf, null);

		int retVal = reader.getNumberOfPages();

		reader.close();

		return retVal;

	}

	private void storeImages(File[] pngFiles, String preffix, String name)
			throws IOException {

		for (File file : pngFiles) {

			GridFSInputFile gfsFile = gfs.createFile(file);

			gfsFile.setContentType("image/png");

			gfsFile.setFilename(preffix + "/"
					+ file.getName().replaceAll("p-0*", ""));

			gfsFile.put("name", name);

			gfsFile.save();

			FileUtils.deleteQuietly(file);

		}

	}

	protected void autoIndexDocument() {
		// Auto indexing
		// FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
		// File[] rules = rulesFolder.listFiles(filter);
		// if (rules != null) {
		// Classifier c = new Classifier(document.getIndexFolder());
		// TreeMap<String, List<Integer>> results = c.analyze(rules);
		// c.close();
		// OutlineHandler outline = new OutlineHandler(results);
		// JSONObject data = new JSONObject();
		// data.put("children", outline.getRoot());
		// document.saveData(data.toString());
	}

	public void indexDocument(String solrHome) throws MalformedURLException,
			IOException, ParserConfigurationException, SAXException,
			EncodingException, EOFException, EntityException, ParseException,
			XPathParseException, NavException, XPathEvalException,
			TranscodeException, SolrServerException {

		String name = document.getFolder().getName();

		SolrIndexer indexer = new SolrIndexer(name,
				"http://localhost:8081/reader-index");

		UpdateResponse ur = indexer.getServer().deleteByQuery("name:" + name);

		System.out.println(ur);

		indexer.indexXMLFile(document.getOptiText());

	}
}
