package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.filefilter.FileFilterUtils;

import com.fabianonunes.reader.tasks.PdfToImageTask;
import com.fabianonunes.reader.tasks.PgmToPngTask;

public class Converter {

	private File pdfFile = new File("/home/fabiano/workdir/converter/pdf.pdf");

	public static void main(String[] args) throws Throwable {

		Converter converter = new Converter();

		converter.convert();

	}

	public void convert() throws Throwable {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		Integer iterations = new Double(Math.ceil(646f / 8f)).intValue();

		Integer step = 8;

		for (int i = 0; i < iterations; i++) {

			PdfToImageTask pdfTask = new PdfToImageTask(pdfFile);

			// PdfToXMLTask xmlTask = new PdfToXMLTask(pdfFile);
			pdfTask.setFirstPage(step * i + 1);
			pdfTask.setTotalPages(step - 1);
			pdfTask.setLastPage(646);

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

		System.out.println("ok");

		executor.shutdown();

		File imagesDir = new File("/home/fabiano/workdir/converter/images/png");

		FileFilter filter = FileFilterUtils.suffixFileFilter(".pgm");

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

	}

}
