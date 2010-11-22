package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.fabianonunes.reader.storage.ReaderDocument;
import com.fabianonunes.reader.tasks.PdfToXMLTask;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.itextpdf.text.pdf.PdfReader;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class Starter {

	protected static File BASE_DIR = new File("/media/TST02/Processos/Convert");

	public static FileFilter pgmFilter = FileFilterUtils
			.suffixFileFilter(".pgm");

	public static void main(String[] args) throws IOException,
			EncodingException, EOFException, EntityException,
			XPathParseException, NavException, XPathEvalException,
			ParseException, InterruptedException {

		Config cfg = new FileSystemXmlConfig(new File(
				"/home/fabiano/grid/hazelcast-1.9/bin/hazelcast.xml"));

		Hazelcast.init(cfg);

		ExecutorService executor = Hazelcast.getExecutorService();

		Thread.sleep(4000);

		String document = "t1";

		File pdfFile = new File(BASE_DIR, document + ".pdf");

		PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());

		int numOfPages = reader.getNumberOfPages();

		reader.close();

		ReaderDocument rdd = ReaderDocument.generateDocument(pdfFile);

		FileUtils.copyFile(rdd.getPdf(), pdfFile);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		Integer iterations = new Double(Math.ceil(numOfPages / 8f)).intValue();

		Integer step = 8;

		for (int i = 0; i < iterations; i++) {

			PdfToXMLTask xmlTask = new PdfToXMLTask(rdd.getFolder()
					.getAbsolutePath());
			xmlTask.setFirstPage(step * i + 1);
			xmlTask.setTotalPages(step - 1);
			xmlTask.setLastPage(numOfPages);

			Future<Integer> task2 = executor.submit(xmlTask);

			tasks.add(task2);

		}

		// for (int i = 0; i < iterations; i++) {
		//
		// PdfToImageTask pdfTask = new PdfToImageTask(rdd.getFolder());
		// pdfTask.setFirstPage(step * i + 1);
		// pdfTask.setTotalPages(step - 1);
		// pdfTask.setLastPage(numOfPages);
		//
		// Future<Integer> task = executor.submit(pdfTask);
		//
		// tasks.add(task);
		//
		// }

		for (Future<Integer> future : tasks) {

			try {
				future.get();
			} catch (Exception e) {
				System.out.println("Error in: " + e.getMessage());
				e.printStackTrace();
			}

		}

		// File dir = new File("/home/fabiano/workdir/converter/text");
		// File fo = new File("/home/fabiano/workdir/converter/text/full.xml");
		// FileUtils.deleteQuietly(fo);
		// FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
		// File[] files = dir.listFiles(filter);
		// XmlAssembler.assemble(files, fo, "//PAGE");
		// File imagesDir = new
		// File("/home/fabiano/workdir/converter/images/png");
		// filter = FileFilterUtils.suffixFileFilter(".pgm");

		// File[] pgmFiles = rdd.getImageFolder().listFiles(pgmFilter);
		//
		// System.out.println(pgmFiles.length);
		//
		// tasks = new LinkedList<Future<Integer>>();
		//
		// for (File pgmImage : pgmFiles) {
		//
		// Future<Integer> task = executor.submit(new PgmToPngTask(rdd,
		// pgmImage));
		//
		// tasks.add(task);
		//
		// }
		//
		// for (Future<Integer> future : tasks) {
		//
		// try {
		// future.get();
		// } catch (Exception e) {
		// System.out.println("Error in: " + e.getMessage());
		// }
		//
		// }

		executor.shutdown();

		Hazelcast.getLifecycleService().shutdown();

	}
}
