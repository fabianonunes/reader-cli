package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.UnknownHostException;
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
import com.fabianonunes.reader.tasks.PdfToImageTask;
import com.fabianonunes.reader.tasks.XmlAssembler;
import com.fabianonunes.reader.text.index.BatchIndexer;
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

	public static void main(String[] args) throws Throwable {

		File inputDir = new File("/media/TST02/Processos/Convert");

		inputDir = new File("/home/fabiano/workdir/converter");

		File[] pdfFiles = inputDir.listFiles(pdfFilter);

		for (File file : pdfFiles) {

			Converter c = new Converter();

			ReaderDocument rdd = ReaderDocument.generateDocument(file);

			long start = System.currentTimeMillis();

			c.convert(rdd);

			System.out.println(System.currentTimeMillis() - start);

			c = null;

			System.gc();

			// break;

		}

	}

	public Converter() throws UnknownHostException, MongoException {

		m = new Mongo();

		db = m.getDB("sesdi2");

		processos = db.getCollection("processos");

		processos.ensureIndex(new BasicDBObject("name", 1).append("unique",
				true));

		gfs = new GridFS(db, "images");

	}

	public static void main2(String[] args) throws Throwable {

		File inputDir = new File("/media/TST02/Processos-Analysys/40-100");
		File indexDir = new File("/media/TST02/Processos-Analysys/40-100/index");
		indexDir.mkdir();

		File[] rddFiles = inputDir.listFiles();

		final BatchIndexer indexer = new BatchIndexer(indexDir);

		ExecutorService executor = Executors.newFixedThreadPool(1);

		LinkedList<Future<String>> tasks = new LinkedList<Future<String>>();

		for (final File file : rddFiles) {

			Future<String> future = executor.submit(new Callable<String>() {

				@Override
				public String call() throws Exception {

					ReaderDocument rdd = new ReaderDocument(file);

					if (rdd.getFullText().exists()) {

						indexer.index(rdd.getFullText(), rdd.getFolder()
								.getName());

					}

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

		indexer.close();

		executor.shutdown();

	}

	public void convert(final ReaderDocument document) throws Throwable {

		File pdfFile = document.getPdf();

		numOfPages = calcNumOfPages(pdfFile);

		//
		BasicDBObject doc = new BasicDBObject();
		doc.append("name", document.getFolder().getName());
		doc.append("pages", numOfPages);
		processos.save(doc);
		//

		ExecutorService executor = Executors.newFixedThreadPool(8);
		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		Integer iterations = new Double(Math.ceil(numOfPages / 8f)).intValue();

		System.out.print("Converting pdf to xml/images...");

		long start = System.currentTimeMillis();

		Integer step = 8;
		for (int i = 0; i < iterations; i++) {

			PdfToImageTask pdfTask = new PdfToImageTask(document);
			pdfTask.setFirstPage(step * i + 1);
			pdfTask.setTotalPages(step - 1);
			pdfTask.setLastPage(numOfPages);

			Future<Integer> task = executor.submit(pdfTask);
			tasks.add(task);

			// PdfToXMLTask xmlTask = new PdfToXMLTask(document);
			// xmlTask.setFirstPage(step * i + 1);
			// xmlTask.setTotalPages(step - 1);
			// xmlTask.setLastPage(numOfPages);
			//
			// Future<Integer> task2 = executor.submit(xmlTask);
			// tasks.add(task2);

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
		
		System.exit(0);

		executor.awaitTermination(20, TimeUnit.MINUTES);

		System.out.println(" [" + ((start - System.currentTimeMillis()) / 1000)
				+ " s]");

		File[] files = document.getTextFolder().listFiles(xmlFilter);

		System.out.print("Merging XML files...");
		start = System.currentTimeMillis();
		XmlAssembler.assemble(files, document.getFullText(), "//PAGE");
		System.out.println(" [" + ((start - System.currentTimeMillis()) / 1000)
				+ " s]");

		//
		System.out.print("Optimizing XML files...");
		start = System.currentTimeMillis();
		File fullFile = document.getFullText();
		OptiXML opti = new OptiXML(fullFile);
		opti.optimize();
		System.out.println(" [" + ((start - System.currentTimeMillis()) / 1000)
				+ " s]");

		//
		System.out.print("Storing images in database...");
		start = System.currentTimeMillis();
		File[] thumbsFiles = document.getThumbsFolder().listFiles(pngFilter);
		File[] pngFiles = document.getImageFolder().listFiles(pngFilter);

		storeImages(pngFiles, document.getFolder().getName());
		storeImages(thumbsFiles, document.getFolder().getName() + "/t");
		System.out.println(" [" + ((start - System.currentTimeMillis()) / 1000)
				+ " s]");

		// List<File> pngs = new ArrayList<File>();
		// pngs.addAll(Arrays.asList(thumbsFiles));
		// for (File pngFile : pngs) {
		// File toFile = new File(pngFile.getParentFile(), pngFile.getName()
		// .replaceAll("p-0*", ""));
		// pngFile.renameTo(toFile);
		// }

		FileUtils.cleanDirectory(document.getIndexFolder());
		// FileUtils.forceDelete(document.getImageFolder());

		System.out.println("Indexing documents...");
		Indexer indexer = new Indexer(document.getIndexFolder(), document
				.getFolder().getName());

		indexer.indexXMLFile(document.getOptiText());

		indexer.close();

		// Auto indexing
		// FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
		// File[] rules = rulesFolder.listFiles(filter);
		//
		// if (rules != null) {
		//
		// Classifier c = new Classifier(document.getIndexFolder());
		// TreeMap<String, List<Integer>> results = c.analyze(rules);
		// c.close();
		//
		// OutlineHandler outline = new OutlineHandler(results);
		// JSONObject data = new JSONObject();
		// data.put("children", outline.getRoot());
		// document.saveData(data.toString());

		System.out.println("Extracting outline/annotations...");
		try {
			document.extractData();
		} catch (Exception e) {
			// queitly
		}

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

}
