package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.fabianonunes.reader.CLIActions;
import com.fabianonunes.reader.storage.ReaderDocument;
import com.fabianonunes.reader.tasks.PdfToImageTask;
import com.fabianonunes.reader.tasks.PdfToXMLTask;
import com.fabianonunes.reader.tasks.PgmToPngTask;
import com.fabianonunes.reader.tasks.XmlAssembler;
import com.fabianonunes.reader.text.index.BatchIndexer;
import com.itextpdf.text.pdf.PdfReader;

public class Converter {

	private FileFilter xmlFilter = FileFilterUtils.suffixFileFilter(".xml");
	private FileFilter pgmFilter = FileFilterUtils.suffixFileFilter(".pgm");
	private FileFilter pngFilter = FileFilterUtils.suffixFileFilter(".png");
	private static FileFilter pdfFilter = FileFilterUtils
			.suffixFileFilter(".pdf");

	public static void main(String[] args) throws Throwable {

		File inputDir = new File(
				"/media/TST02/Processos-Analysys/reclamacao-trabalhista");

		File[] pdfFiles = inputDir.listFiles(pdfFilter);

		for (File file : pdfFiles) {

			Converter c = new Converter();

			c.convert(ReaderDocument.generateDocument(file));

		}

	}

	public void convert(ReaderDocument document) throws Throwable {

		File pdfFile = document.getPdf();

		ExecutorService executor = Executors.newFixedThreadPool(10);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());

		int numOfPages = reader.getNumberOfPages();

		reader.close();

		Integer iterations = new Double(Math.ceil(numOfPages / 10f)).intValue();

		Integer step = 10;

		for (int i = 0; i < iterations; i++) {

			PdfToImageTask pdfTask = new PdfToImageTask(document);
			pdfTask.setFirstPage(step * i + 1);
			pdfTask.setTotalPages(step - 1);
			pdfTask.setLastPage(numOfPages);

			PdfToXMLTask xmlTask = new PdfToXMLTask(document);
			xmlTask.setFirstPage(step * i + 1);
			xmlTask.setTotalPages(step - 1);
			xmlTask.setLastPage(numOfPages);

			Future<Integer> task = executor.submit(pdfTask);
			Future<Integer> task2 = executor.submit(xmlTask);

			tasks.add(task);
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

		File[] files = document.getTextFolder().listFiles(xmlFilter);

		XmlAssembler.assemble(files, document.getFullText(), "//PAGE");

		CLIActions.simpleXML(document.getFullText());
		CLIActions.optimizeXML(document.getFullText());

		File[] pgmFiles = document.getImageFolder().listFiles(pgmFilter);

		executor = Executors.newFixedThreadPool(4);

		tasks = new LinkedList<Future<Integer>>();

		for (File pgmImage : pgmFiles) {

			Future<Integer> task = executor.submit(new PgmToPngTask(pgmImage));

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

		BatchIndexer indexer = new BatchIndexer(document.getIndexFolder());

		indexer.index(document.getFullText());
		indexer.close();

	}

}
