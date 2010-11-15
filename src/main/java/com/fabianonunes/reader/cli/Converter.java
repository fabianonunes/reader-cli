package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.fabianonunes.reader.tasks.PdfToImageTask;
import com.fabianonunes.reader.tasks.PdfToXMLTask;
import com.fabianonunes.reader.tasks.PgmToPngTask;
import com.fabianonunes.reader.tasks.XmlAssembler;

public class Converter {

	private File pdfFile = new File("/home/fabiano/workdir/converter/pdf.pdf");

	public static void main(String[] args) throws Throwable {

		Converter converter = new Converter();

		converter.convert();

	}

	public void convert() throws Throwable {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		Integer numOfPages = 100;

		Integer iterations = new Double(Math.ceil(numOfPages / 8f)).intValue();

		Integer step = 8;

		for (int i = 0; i < iterations; i++) {

			PdfToImageTask pdfTask = new PdfToImageTask(pdfFile);
			pdfTask.setFirstPage(step * i + 1);
			pdfTask.setTotalPages(step - 1);
			pdfTask.setLastPage(numOfPages);

			PdfToXMLTask xmlTask = new PdfToXMLTask(pdfFile);
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

		File dir = new File("/home/fabiano/workdir/converter/text");

		File fo = new File("/home/fabiano/workdir/converter/text/full.xml");

		FileUtils.deleteQuietly(fo);

		FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");

		File[] files = dir.listFiles(filter);

		XmlAssembler.assemble(files, fo, "//PAGE");

		File imagesDir = new File("/home/fabiano/workdir/converter/images/png");

		filter = FileFilterUtils.suffixFileFilter(".pgm");

		File[] pgmFiles = imagesDir.listFiles(filter);

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

	}

}
