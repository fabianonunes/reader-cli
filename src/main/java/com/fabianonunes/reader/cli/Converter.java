package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.fabianonunes.reader.pdf.outline.OutlineHandler;
import com.fabianonunes.reader.pdf.text.position.OptiXML;
import com.fabianonunes.reader.pdf.text.position.SimpleXML;
import com.fabianonunes.reader.storage.ReaderDocument;
import com.fabianonunes.reader.tasks.PdfToImageTask;
import com.fabianonunes.reader.tasks.PdfToXMLTask;
import com.fabianonunes.reader.tasks.PgmToPngTask;
import com.fabianonunes.reader.tasks.XmlAssembler;
import com.fabianonunes.reader.text.classification.Classifier;
import com.fabianonunes.reader.text.index.BatchIndexer;
import com.fabianonunes.reader.text.index.Indexer;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;

public class Converter {

	private FileFilter xmlFilter = FileFilterUtils.suffixFileFilter(".xml");
	public FileFilter pgmFilter = FileFilterUtils.suffixFileFilter(".pgm");
	public FileFilter pngFilter = FileFilterUtils.suffixFileFilter(".png");
	private static FileFilter pdfFilter = FileFilterUtils
			.suffixFileFilter(".pdf");

	private static File rulesFolder = new File(
			"/home/fabiano/workdir/reader-tests/rules");

	public static void main3(String[] args) throws Throwable {

		File inputDir = new File("/media/TST02/Processos/Convert/");

		// inputDir = new File("/home/fabiano/workdir/converter");

		File[] pdfFiles = inputDir.listFiles(pdfFilter);

		for (File file : pdfFiles) {

			Converter c = new Converter();

			ReaderDocument rdd = ReaderDocument.generateDocument(file);

			c.convert(rdd);

			c = null;

			System.gc();

		}

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

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		RandomAccessFileOrArray raf = new RandomAccessFileOrArray(
				pdfFile.getAbsolutePath());

		PdfReader reader = new PdfReader(raf, null);

		int numOfPages = reader.getNumberOfPages();

		reader.close();

		Integer iterations = new Double(Math.ceil(numOfPages / 8f)).intValue();

		Integer step = 8;

		System.out.println("Converting pdf to images...");
		for (int i = 0; i < iterations; i++) {

			PdfToImageTask pdfTask = new PdfToImageTask(document);
			pdfTask.setFirstPage(step * i + 1);
			pdfTask.setTotalPages(step - 1);
			pdfTask.setLastPage(numOfPages);

			Future<Integer> task = executor.submit(pdfTask);
			tasks.add(task);

		}

		System.out.println("Converting pdf to xml...");
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

		executor.awaitTermination(3, TimeUnit.MINUTES);

		System.gc();

		executor = Executors.newSingleThreadExecutor();

		final File[] files = document.getTextFolder().listFiles(xmlFilter);

		Future<Integer> xmlTasks = executor.submit(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {

				XmlAssembler.assemble(files, document.getFullText(), "//PAGE");

				// simple-xml
				File fullFile = document.getFullText();
				SimpleXML xmlt = new SimpleXML(fullFile);
				xmlt.convert();
				XMLOutputter outputter = new XMLOutputter(Format
						.getPrettyFormat());
				FileWriter w = new FileWriter(new File(
						fullFile.getParentFile(), "text.xml").getAbsolutePath());
				outputter.output(xmlt.getDoc(), w);
				xmlt.resetDocument();
				xmlt = null;
				w.close();

				// opti-xml
				OptiXML opti = new OptiXML(fullFile);
				opti.optimize();
				opti = null;

				return null;
			}

		});

		xmlTasks.get();

		executor.shutdown();

		executor.awaitTermination(3, TimeUnit.MINUTES);

		System.gc();

		executor = Executors.newFixedThreadPool(4);

		File[] pgmFiles = document.getImageFolder().listFiles(pgmFilter);

		tasks = new LinkedList<Future<Integer>>();

		System.out.println("Converting pgm to png...");

		for (File pgmImage : pgmFiles) {

			Future<Integer> task = executor.submit(new PgmToPngTask(document,
					pgmImage));

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

		executor.awaitTermination(3, TimeUnit.MINUTES);

		System.gc();

		File[] pngFiles = document.getImageFolder().listFiles(pngFilter);

		File[] thumbsFiles = document.getThumbsFolder().listFiles(pngFilter);

		List<File> pngs = new ArrayList<File>();
		pngs.addAll(Arrays.asList(pngFiles));
		pngs.addAll(Arrays.asList(thumbsFiles));

		for (File pngFile : pngs) {

			File toFile = new File(pngFile.getParentFile(), pngFile.getName()
					.replaceAll("p-0*", ""));

			pngFile.renameTo(toFile);
		}

		FileUtils.cleanDirectory(document.getIndexFolder());

		Indexer indexer = new Indexer(document.getIndexFolder(), document
				.getFolder().getName());

		indexer.indexXMLFile(document.getOptiText());

		indexer.close();

		// Auto indexing
		FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
		File[] rules = rulesFolder.listFiles(filter);

		if (rules != null) {

			Classifier c = new Classifier(document.getIndexFolder());
			TreeMap<String, List<Integer>> results = c.analyze(rules);
			c.close();

			OutlineHandler outline = new OutlineHandler(results);
			JSONObject data = new JSONObject();
			data.put("children", outline.getRoot());
			document.saveData(data.toString());

			try {
				document.extractData();
			} catch (Exception e) {
				// queitly
			}

		}

	}

}
