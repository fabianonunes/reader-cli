package com.fabianonunes.reader.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;

import com.fabianonunes.reader.storage.ReaderDocument;

public class PdfToXMLTask implements Callable<Integer>, Serializable {

	private static final long serialVersionUID = 1L;

	private Integer firstPage;
	private Integer totalPages;
	private Integer lastPage;

	private File rddFolder;

	private String path;

	public PdfToXMLTask(ReaderDocument document) {

		rddFolder = document.getFolder();

	}

	public PdfToXMLTask(File folder) throws IOException {
		rddFolder = folder;
	}

	public PdfToXMLTask(String absolutePath) {
		this.path = absolutePath;
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

		if (this.path != null) {
			rddFolder = new File(path);
		}

		ReaderDocument document = new ReaderDocument(rddFolder);

		File pdfFile = document.getPdf();

		File outputDir = document.getTextFolder();

		if (firstPage == null || lastPage == null || totalPages == null) {
			throw new InvalidParameterException();
		}

		File output = new File(outputDir, "p" + firstPage + ".xml");

		Integer lastPage = Math.min(this.lastPage, (firstPage + totalPages));

		String command = "pdftoxml -noImage -noImageInline" + " -f "
				+ firstPage + " -l " + lastPage + " "
				+ pdfFile.getAbsolutePath() + " " + output.getAbsolutePath();

		CommandLine cmdLine = CommandLine.parse(command);

		// ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 60 * 1000);
		Executor exec = new DefaultExecutor();
		// exec.setWatchdog(watchdog);
		exec.setExitValue(0);
		exec.setStreamHandler(new ExecuteStreamHandler() {

			@Override
			public void stop() {
				// TODO Auto-generated method stub

			}

			@Override
			public void start() throws IOException {
				// TODO Auto-generated method stub

			}

			@Override
			public void setProcessOutputStream(InputStream is)
					throws IOException {
				// TODO Auto-generated method stub

			}

			@Override
			public void setProcessInputStream(OutputStream os)
					throws IOException {
				// TODO Auto-generated method stub

			}

			@Override
			public void setProcessErrorStream(InputStream is)
					throws IOException {
				// TODO Auto-generated method stub

			}
		});

		exec.execute(cmdLine);

		command = "tidy -utf8 -xml -w 255 -i -c -q -asxml -o "
				+ output.getAbsolutePath() + " " + output.getAbsolutePath();

		cmdLine = CommandLine.parse(command);

		exec.execute(cmdLine);

		return null;

	}

}
