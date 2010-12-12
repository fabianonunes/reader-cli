package com.fabianonunes.reader.tasks;

import java.io.File;
import java.io.IOException;
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
		
		try {
			p.getErrorStream().close();
		} catch (Exception e) {
			// quietly
		}
		
		try {
			p.getInputStream().close();
		} catch (Exception e) {
			// quietly
		}


		command = "tidy -utf8 -xml -w 255 -i -c -q -asxml -o "
				+ output.getAbsolutePath() + " " + output.getAbsolutePath();

		p = runtime.exec(command);

		p.waitFor();

		p.destroy();
		
		try {
			p.getErrorStream().close();
		} catch (Exception e) {
			// quietly
		}
		
		try {
			p.getInputStream().close();
		} catch (Exception e) {
			// quietly
		}


		return null;

	}

}
