package com.fabianonunes.reader.tasks;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.fabianonunes.reader.cli.Converter;

public class BZipper {
	
	public static void mainbzip(String[] args) throws InterruptedException {

		ExecutorService executor = Executors.newFixedThreadPool(4);

		LinkedList<Future<Integer>> tasks = new LinkedList<Future<Integer>>();

		File dir = new File("/media/TST02/Digitalizacao-RAW-Baixados");
		File[] files = dir.listFiles(Converter.pdfFilter);

		for (final File file : files) {

			Future<Integer> task = executor.submit(new Callable<Integer>() {

				@Override
				public Integer call() throws Exception {

					DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

					ByteArrayOutputStream os = new ByteArrayOutputStream();

					String command = "bzip2 -z -v -9 -f \""
							+ file.getAbsolutePath() + "\"";

					/**/
					CommandLine cmdLine = CommandLine.parse(command);

					ExecuteWatchdog watchdog = new ExecuteWatchdog(
							10 * 60 * 1000);
					Executor exec = new DefaultExecutor();
					exec.setWorkingDirectory(file.getParentFile());
					exec.setWatchdog(watchdog);
					exec.setExitValue(0);

					exec.execute(cmdLine, resultHandler);

					exec.setStreamHandler(new PumpStreamHandler(os));

					resultHandler.waitFor();

					System.out.println(new String(os.toByteArray()));

					return null;
				}

			});

			tasks.add(task);

		}

		for (Future<Integer> future : tasks) {

			try {
				future.get();
			} catch (Exception e) {
				System.out.println("Error in: " + e.getMessage());
				e.printStackTrace();
			}

		}

		executor.shutdown();

		executor.awaitTermination(20, TimeUnit.MINUTES);

	}


}
