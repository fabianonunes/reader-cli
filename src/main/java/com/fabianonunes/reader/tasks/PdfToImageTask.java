package com.fabianonunes.reader.tasks;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

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

		String command = "pdftoppm -r 110" + //
				" -f " + firstPage + //
				" -l " + lastPage + //
				" -gray " + //
				pdfFile.getAbsolutePath() + //
				" " + outputDir.getAbsolutePath() + "/p";

		return command;

	}

	@Override
	public Integer call() throws Exception {

		exec(getCommand());

		// Integer lastPage = Math.min(this.lastPage, (firstPage + totalPages));
		//
		// ArrayList<File> rawFiles = new ArrayList<File>();
		//
		// Double numOfChars =
		// Math.floor(Math.log10(this.lastPage.doubleValue())) + 1;
		//
		// for (int i = firstPage; i <= lastPage; i++) {
		//
		// String name = StringUtils.leftPad(Integer.toString(i),
		// numOfChars.intValue(), "0");
		//
		// rawFiles.add(new File(outputDir, "p-" + name + ".pgm"));
		//
		// }
		//
		// for (File pgmFile : rawFiles) {
		//
		// String command;
		//
		// if (pgmFile == null || !pgmFile.isFile()) {
		// continue;
		// }
		//
		// File tDir = new File(pgmFile.getParentFile(), "t");
		//
		// if (!tDir.exists()) {
		// tDir.mkdir();
		// }
		//
		// String outName = pgmFile.getName().replaceAll("p-0*", "")
		// .replace("pgm", "png");
		//
		// File pFile = new File(pgmFile.getParentFile(), outName);
		// File tFile = new File(tDir, outName);
		//
		// command = "convert -depth 2 " + pgmFile + " " + pFile;
		// exec(command);
		//
		// command = "convert -resize 200x -depth 2 " + pFile + " " + tFile;
		// exec(command);
		//
		// pgmFile.delete();
		//
		// }

		return null;

	}

	private void exec(String command) throws ExecuteException, IOException,
			InterruptedException {

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		CommandLine cmdLine = CommandLine.parse(command);

		Executor exec = new DefaultExecutor();
		// ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
		// exec.setWatchdog(watchdog);
		exec.setExitValue(0);

		exec.execute(cmdLine, resultHandler);

		resultHandler.waitFor();

	}

}
