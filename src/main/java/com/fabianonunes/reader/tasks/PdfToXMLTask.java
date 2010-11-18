package com.fabianonunes.reader.tasks;

import java.io.File;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;

import com.fabianonunes.reader.storage.ReaderDocument;

public class PdfToXMLTask implements Callable<Integer>, Serializable {

	private static final long serialVersionUID = 1L;

	private static Runtime runtime = Runtime.getRuntime();
	private Integer firstPage;
	private Integer totalPages;
	private Integer lastPage;
	private ReaderDocument rdd;

	public PdfToXMLTask(ReaderDocument document) {

		this.rdd = document;

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

		File pdfFile = rdd.getPdf();

		File outputDir = rdd.getTextFolder();

		System.out.println("xml");

		if (firstPage == null || lastPage == null || totalPages == null) {
			throw new InvalidParameterException();
		}

		File output = new File(outputDir, "p" + firstPage + ".xml");

		Integer lastPage = Math.min(this.lastPage, (firstPage + totalPages));

		String command;

		Process p;

		command = "pdftoxml -noImage -noImageInline" + " -f " + firstPage
				+ " -l " + lastPage + " " + pdfFile.getAbsolutePath() + " "
				+ output.getAbsolutePath();

		String osName = System.getProperty("os.name");

		if (osName.contains("Windows")) {

			String[] cmd = new String[3];
			cmd[0] = "cmd.exe";
			cmd[1] = "/C";
			cmd[2] = command;

			p = runtime.exec(cmd);

			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(),
					"ERROR");

			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(),
					"OUTPUT");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

		} else {

			p = runtime.exec(command);
		}

		p.waitFor();

		p.destroy();

		command = "tidy -utf8 -xml -w 255 -i -c -q -asxml -o "
				+ output.getAbsolutePath() + " " + output.getAbsolutePath();

		p = runtime.exec(command);

		p.waitFor();

		p.destroy();

		return null;

	}

	
}
