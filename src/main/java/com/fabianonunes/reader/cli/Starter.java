package com.fabianonunes.reader.cli;


public class Starter {

	public static void main(String[] args) {

		// File pdfFile = new
		// File("/home/fabiano/workdir/converter/27700-02.2009.5.22.0000.pdf");
		//
		// ExecutorService executor = Hazelcast.getExecutorService();
		//
		// LinkedList<Future<Integer>> tasks = new
		// LinkedList<Future<Integer>>();
		//
		// Integer numOfPages = 640;
		//
		// Integer iterations = new Double(Math.ceil(numOfPages /
		// 8f)).intValue();
		//
		// Integer step = 8;
		//
		// for (int i = 0; i < iterations; i++) {
		//
		// PdfToImageTask pdfTask = new PdfToImageTask(pdfFile);
		// pdfTask.setFirstPage(step * i + 1);
		// pdfTask.setTotalPages(step - 1);
		// pdfTask.setLastPage(numOfPages);
		//
		// PdfToXMLTask xmlTask = new PdfToXMLTask(pdfFile);
		// xmlTask.setFirstPage(step * i + 1);
		// xmlTask.setTotalPages(step - 1);
		// xmlTask.setLastPage(numOfPages);
		//
		// Future<Integer> task = executor.submit(pdfTask);
		// Future<Integer> task2 = executor.submit(xmlTask);
		//
		// tasks.add(task);
		// tasks.add(task2);
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
		//
		// File dir = new File("/home/fabiano/workdir/converter/text");
		//
		// File fo = new File("/home/fabiano/workdir/converter/text/full.xml");
		//
		// FileUtils.deleteQuietly(fo);
		//
		// FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
		//
		// File[] files = dir.listFiles(filter);
		//
		// XmlAssembler.assemble(files, fo, "//PAGE");
		//
		// File imagesDir = new
		// File("/home/fabiano/workdir/converter/images/png");
		//
		// filter = FileFilterUtils.suffixFileFilter(".pgm");
		//
		// File[] pgmFiles = imagesDir.listFiles(filter);
		//
		// tasks = new LinkedList<Future<Integer>>();
		//
		// for (File pgmImage : pgmFiles) {
		//
		// Future<Integer> task = executor.submit(new PgmToPngTask(pgmImage));
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
		//
		// Hazelcast.getLifecycleService().shutdown();

	}
}
