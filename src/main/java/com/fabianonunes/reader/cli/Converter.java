package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.fabianonunes.reader.pdf.text.position.OptiXML;
import com.fabianonunes.reader.storage.ReaderDocument;
import com.fabianonunes.reader.tasks.CommandLineExecutor;
import com.fabianonunes.reader.tasks.PdfToImageTask;
import com.fabianonunes.reader.tasks.PdfToXMLTask;
import com.fabianonunes.reader.tasks.XmlAssembler;
import com.fabianonunes.reader.text.index.Indexer;
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

	private long timer;

	private ReaderDocument document;

	public static void main(String[] args) throws Throwable {

		File inputDir = new File("/media/TST02/Processos/Convert");

		inputDir = new File("/home/fabiano/workdir/converter");

		File[] pdfFiles = inputDir.listFiles(pdfFilter);

		for (File file : pdfFiles) {

			ReaderDocument rdd = ReaderDocument.generateDocument(file);

			Converter c = new Converter(rdd);

			c.convert();

			// break;

		}

	}

	public Converter(ReaderDocument document) throws MongoException,
			IOException {

		this.document = document;

		File pdfFile = document.getPdf();

		numOfPages = calcNumOfPages(pdfFile);

		m = new Mongo();// "10.0.223.163"

		db = m.getDB("sesdi2");

		// db.authenticate("fabiano_sesdi2", "timestamp-2010".toCharArray());

		processos = db.getCollection("processos");

		processos.ensureIndex(new BasicDBObject("name", 1).append("unique",
				true));

		gfs = new GridFS(db, "images");

	}

	public void convert() throws Throwable {
		//
		startTimer();
		extractImages();
		endTimer();

		optimizeImages();
		//
		startTimer();
		extractText();
		endTimer();
		//
		startTimer();
		manipulateTextFiles();
		endTimer();
		// //
		// startTimer();
		storeToDB();
		// endTimer();
		// //
		// startTimer();
		// indexDocument(document);
		// endTimer();
		//
		// // startTimer();
		// // autoIndexDocument(document);
		// // endTimer();
		//
		// startTimer();
		extractPdfData();
		// endTimer();

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

		indexer.close();

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

		BasicDBObject doc = new BasicDBObject();
		doc.append("name", document.getFolder().getName());
		doc.append("pages", numOfPages);
		processos.save(doc);

		// storing images

		File[] thumbsFiles = document.getThumbsFolder().listFiles(pngFilter);
		File[] pngFiles = document.getImageFolder().listFiles(pngFilter);
		File[] xmlGzFiles = document.getSimpleTextFolder().listFiles();

		storeImages(pngFiles, document.getFolder().getName());
		storeImages(thumbsFiles, document.getFolder().getName() + "/t");
		storeImages(xmlGzFiles, document.getFolder().getName() + "/s");
		
		// FileUtils.cleanDirectory(document.getIndexFolder());
		// FileUtils.forceDelete(document.getImageFolder());

	}

	private Integer calcNumOfPages(File pdfFile) throws IOException {

		RandomAccessFileOrArray raf = new RandomAccessFileOrArray(
				pdfFile.getAbsolutePath());

		PdfReader reader = new PdfReader(raf, null);

		int retVal = reader.getNumberOfPages();

		reader.close();

		return retVal;

	}

	private void storeImages(File[] pngFiles, String preffix)
			throws IOException {

		for (File file : pngFiles) {

			GridFSInputFile gfsFile = gfs.createFile(file);

			gfsFile.setContentType("image/png");

			gfsFile.setFilename(preffix + "/"
					+ file.getName().replaceAll("p-0*", ""));

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

	private void startTimer() {

		timer = System.currentTimeMillis();

	}

	private void endTimer() {

		long c = System.currentTimeMillis();

		Double r = (c - timer) / 1000d;

		System.out.println("[" + r + "secs]");

	}

}
