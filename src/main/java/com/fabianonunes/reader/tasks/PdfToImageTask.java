package com.fabianonunes.reader.tasks;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;

public class PdfToImageTask implements Callable<Integer> {

	private static Runtime runtime = Runtime.getRuntime();
	private Integer firstPage;
	private Integer totalPages;
	private Integer lastPage;

	private File pdfFile;
	private File outputDir;

	public PdfToImageTask(File pdfFile) {

		this.pdfFile = pdfFile;

		outputDir = new File(pdfFile.getParentFile(), "images");
		outputDir.mkdir();

		outputDir = new File(outputDir, "png");
		outputDir.mkdir();

	}

	public Integer getFirstPage() {
		return firstPage;
	}

	public void setFirstPage(Integer firstPage) {
		this.firstPage = firstPage;
	}

	public Integer getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(Integer totalPages) {
		this.totalPages = totalPages;
	}

	public Integer getLastPage() {
		return lastPage;
	}

	public void setLastPage(Integer lastPage) {
		this.lastPage = lastPage;
	}

	@Override
	public Integer call() throws Exception {

		if (firstPage == null || lastPage == null || totalPages == null) {
			throw new InvalidParameterException();
		}

		Integer lastPage = Math.min(this.lastPage, (firstPage + totalPages));

		String command = "pdftoppm -r 300" + //
				" -f " + firstPage + //
				" -l " + lastPage + //
				" -gray -scale-to 1000 " + //
				pdfFile.getAbsolutePath() + //
				" " + outputDir.getAbsolutePath() + "/p";

		Process p = runtime.exec(command);

		return p.waitFor();

	}
}
