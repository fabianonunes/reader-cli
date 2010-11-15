package com.fabianonunes.reader.tasks;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;

public class PgmToPngTask implements Callable<Integer> {

	private static Runtime runtime = Runtime.getRuntime();

	private File pgmFile;

	public PgmToPngTask(File pgmImage) {

		this.pgmFile = pgmImage;

	}

	@Override
	public Integer call() throws Exception {

		if (pgmFile == null || !pgmFile.isFile()) {
			throw new InvalidParameterException();
		}

		File pngFile = new File(pgmFile.getAbsolutePath().replace(".pgm",
				".png"));

		File tDir = new File(pgmFile.getParentFile(), "t");
		tDir.mkdir();

		File tFile = new File(tDir, pngFile.getName());

		String command = "convert " + pgmFile.getAbsolutePath() + " "
				+ pngFile.getAbsolutePath();

		Process p = runtime.exec(command);

		p.waitFor();

		command = "convert " + pngFile + " -resize x200 "
				+ tFile.getAbsolutePath();

		p = runtime.exec(command);

		return p.waitFor();

	}
}
