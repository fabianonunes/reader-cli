package com.fabianonunes.reader.tasks;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.lang.StringUtils;

import com.fabianonunes.reader.storage.ReaderDocument;

public class PdfToImageTask implements Callable<Integer>, Serializable {

	private static final long serialVersionUID = 1L;

	private Integer firstPage;
	private Integer totalPages;
	private Integer lastPage;

	private File pdfFile;
	private File outputDir;

	public PdfToImageTask(ReaderDocument document) {

		pdfFile = document.getPdf();

		outputDir = document.getImageFolder();

	}

	public PdfToImageTask(File folder) throws IOException {
		this(new ReaderDocument(folder));
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

	public String getCommand() {

		if (firstPage == null || lastPage == null || totalPages == null) {
			throw new InvalidParameterException();
		}

		Integer lastPage = Math.min(this.lastPage, (firstPage + totalPages));

		String command = "pdftoppm -r 200" + //
				" -f " + firstPage + //
				" -l " + lastPage + //
				" -png -scale-to 1000 " + //
				pdfFile.getAbsolutePath() + //
				" " + outputDir.getAbsolutePath() + "/p";

		return command;

	}

	@Override
	public Integer call() throws Exception {

		exec(getCommand());

		// =-=-=-=-=-=-=-=-=-=-=-=-=-

		Integer lastPage = Math.min(this.lastPage, (firstPage + totalPages));

		ArrayList<File> rawFiles = new ArrayList<File>();

		Double chars = Math.floor(Math.log10(this.lastPage.doubleValue())) + 1;

		for (int i = firstPage; i <= lastPage; i++) {

			String name = StringUtils.leftPad(Integer.toString(i),
					chars.intValue(), "0");

			rawFiles.add(new File(outputDir, "p-" + name + ".png"));

		}

		for (File rawFile : rawFiles) {

			if (rawFile == null || !rawFile.isFile()) {
				System.out.println(rawFile);
				throw new InvalidParameterException();
			}

			File tDir = new File(rawFile.getParentFile(), "t");

			if (!tDir.exists()) {
				tDir.mkdir();
			}

			File tFile = new File(tDir, rawFile.getName()
					.replaceAll("p-0*", ""));

			String command = "convert -depth 2 " + rawFile + " -resize x200 "
					+ tFile.getAbsolutePath();

			exec(command);

		}

		for (File file : rawFiles) {

			String name = file.getName();

			File outFile = new File(file.getParentFile(), name.replaceAll(
					"p-0*", ""));

			String command = "convert -depth 2 " + file.getAbsolutePath() + " "
					+ outFile;

			exec(command);

			file.delete();

		}

		return null;

	}

	private void exec(String command) throws ExecuteException, IOException,
			InterruptedException {

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		CommandLine cmdLine = CommandLine.parse(command);

		// ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
		Executor exec = new DefaultExecutor();
		// exec.setWatchdog(watchdog);
		exec.setExitValue(0);

		exec.execute(cmdLine, resultHandler);

		resultHandler.waitFor();

	}

}
