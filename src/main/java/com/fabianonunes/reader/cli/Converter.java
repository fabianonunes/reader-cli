package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jdom.JDOMException;

import com.fabianonunes.reader.pdf.text.position.OptiXML;
import com.fabianonunes.reader.storage.ReaderDocument;
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

	public static void main(String[] args) throws Throwable {

		File inputDir = new File("/media/TST02/Processos/Convert");

		inputDir = new File("/home/fabiano/workdir/converter");

		File[] pdfFiles = inputDir.listFiles(pdfFilter);

		for (File file : pdfFiles) {

			Converter c = new Converter();

			ReaderDocument rdd = ReaderDocument.generateDocument(file);

			long start = System.currentTimeMillis();

			c.convert(rdd);

			System.out.println((System.currentTimeMillis() - start) / 1000d);

			c = null;

			System.gc();

			// break;

		}

	}

	public Converter() throws UnknownHostException, MongoException {

		m = new Mongo();// "10.0.223.163"

		db = m.getDB("sesdi2");

		// db.authenticate("fabiano_sesdi2", "timestamp-2010".toCharArray());

		processos = db.getCollection("processos");

		processos.ensureIndex(new BasicDBObject("name", 1).append("unique",
				true));

		gfs = new GridFS(db, "images");

	}

	public void convert(ReaderDocument document) throws Throwable {

		File pdfFile = document.getPdf();

		numOfPages = calcNumOfPages(pdfFile);
		//
		startTimer();
		extractImages(document);
		endTimer();
		//
		startTimer();
		extractText(document);
		endTimer();
		//
		startTimer();
		manipulateTextFiles(document);
		endTimer();
//		//
//		startTimer();
//		addDocToDB(document);
//		endTimer();
//		//
//		startTimer();
//		indexDocument(document);
//		endTimer();
//
//		// startTimer();
//		// autoIndexDocument(document);
//		// endTimer();
//
//		startTimer();
		extractPdfData(document);
//		endTimer();

	}

	private void extractText(ReaderDocument document)
			throws InterruptedException {

		System.out.print("Converting pdf to xml... ");

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

	protected void extractPdfData(ReaderDocument document) {
		System.out.print("Extracting outline/annotations... ");
		try {
			document.extractData();
		} catch (Exception e) {
			// queitly
		}
	}

	protected void indexDocument(ReaderDocument document) throws IOException,
			EncodingException, EOFException, EntityException, ParseException,
			XPathParseException, NavException, XPathEvalException,
			JDOMException {

		System.out.print("Indexing documents... ");
		Indexer indexer = new Indexer(document.getIndexFolder(), document
				.getFolder().getName());

		indexer.indexXMLFile(document.getOptiText());

		indexer.close();

	}

	private void manipulateTextFiles(ReaderDocument document)
			throws EncodingException, EOFException, EntityException,
			XPathParseException, NavException, XPathEvalException,
			ParseException, IOException, ModifyException, TranscodeException {

		File[] files = document.getTextFolder().listFiles(xmlFilter);

		XmlAssembler.assemble(files, document.getFullText(), "//PAGE");

		File fullFile = document.getFullText();
		OptiXML opti = new OptiXML(fullFile);
		opti.optimize();

	}

	private void extractImages(ReaderDocument document)
			throws InterruptedException {

		System.out.print("Converting pdf to images... ");

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

	protected void addDocToDB(ReaderDocument document) throws IOException {

		System.out.print("Storing images/text in database... ");

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
		FileUtils.cleanDirectory(document.getIndexFolder());
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

	protected void autoIndexDocument(ReaderDocument document) {
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
