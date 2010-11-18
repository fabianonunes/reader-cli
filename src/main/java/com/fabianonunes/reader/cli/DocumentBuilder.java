package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import com.fabianonunes.reader.pdf.stripper.PageExtractor;

public class DocumentBuilder {

	List<PDPage> pages;
	List<File> files;

	public DocumentBuilder(File input) throws InterruptedException,
			ExecutionException {

		files = new ArrayList<File>();

		pages = Collections.synchronizedList(new ArrayList<PDPage>());

		if (input.isDirectory()) {

			FileFilter filter = FileFilterUtils.suffixFileFilter("pdf",
					IOCase.INSENSITIVE);

			for (File f : input.listFiles(filter)) {

				files.add(f);

			}

		}

	}

	public Collection<PDPage> iterate(final String pattern, final File fileDir) {

		List<PDPage> retVal = new ArrayList<PDPage>();

		ExecutorService executor = Executors.newFixedThreadPool(8);

		LinkedList<Future<List<PDPage>>> tasks = new LinkedList<Future<List<PDPage>>>();

		for (final File file : files) {

			Future<List<PDPage>> future = executor
					.submit(new Callable<List<PDPage>>() {

						@Override
						public List<PDPage> call() throws Exception {

							System.out.println(file);

							File outputFile = new File(fileDir, file.getName());

							if (outputFile.exists()) {
								return null;
							}

							PDDocument pddoc = PDDocument.load(file);

							PageExtractor extractor = new PageExtractor(pddoc);

							try {

								List<PDPage> pdpages = extractor
										.filterPagesByBookmarkName(pattern);

								if (pdpages.size() == 0) {
									return null;
								} else {

									PDDocument newPddoc = new PDDocument();

									for (PDPage pdpage : pdpages) {
										newPddoc.importPage(pdpage);
									}

									OutputStream os = new FileOutputStream(
											outputFile);

									newPddoc.save(os);

									newPddoc.close();

									return pdpages;

								}

							} catch (Exception e) {

								System.out.println("Error: " + file);

								e.printStackTrace();

								throw e;

							} finally {

								pddoc.close();

							}

						}

					});

			tasks.add(future);

		}

		for (Future<List<PDPage>> future : tasks) {

			try {
				List<PDPage> v = future.get();
				if (v != null) {
					retVal.addAll(v);
				}
			} catch (Exception e) {
				System.out.println("Error in: " + e.getMessage());
				e.printStackTrace();
			}

		}

		executor.shutdown();

		return retVal;

	}
}
