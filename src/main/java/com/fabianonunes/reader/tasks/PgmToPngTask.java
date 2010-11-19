package com.fabianonunes.reader.tasks;

import java.io.File;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;

import com.fabianonunes.reader.storage.ReaderDocument;

public class PgmToPngTask implements Callable<Integer>, Serializable {

	private static final long serialVersionUID = 1L;

	private static Runtime runtime = Runtime.getRuntime();

	private String pgmFileName;

	private File imageFolder;

	public PgmToPngTask(ReaderDocument document, File pgmImage) {

		this.imageFolder = document.getImageFolder();

		this.pgmFileName = pgmImage.getName();

	}

	@Override
	public Integer call() throws Exception {

		File pgmFile = new File(imageFolder, pgmFileName);

		System.out.println("Converting " + pgmFile.getName() + "...");

		if (pgmFile == null || !pgmFile.isFile()) {
			throw new InvalidParameterException();
		}

		File pngFile = new File(pgmFile.getAbsolutePath().replace(".pgm",
				".png"));

		File tDir = new File(pgmFile.getParentFile(), "t");
		if (!tDir.exists()) {
			tDir.mkdir();
		}

		File tFile = new File(tDir, pngFile.getName());

		String command = "convert " + pgmFile.getAbsolutePath() + " "
				+ pngFile.getAbsolutePath();

		Process p = runtime.exec(command);

		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(),
				"ERROR");

		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(),
				"OUTPUT");

		errorGobbler.start();
		outputGobbler.start();

		p.waitFor();

		p.destroy();

		FileUtils.deleteQuietly(pgmFile);

		command = "convert " + pngFile + " -resize x200 "
				+ tFile.getAbsolutePath();

		p = runtime.exec(command);

		p.waitFor();

		p.destroy();

		return null;

	}
}
