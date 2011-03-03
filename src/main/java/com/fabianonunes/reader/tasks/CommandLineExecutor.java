package com.fabianonunes.reader.tasks;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

public class CommandLineExecutor {

	public static void exec(String command) throws ExecuteException,
			IOException, InterruptedException {

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		CommandLine cmdLine = CommandLine.parse(command);

		Executor exec = new DefaultExecutor();
		exec.setExitValue(0);

		exec.execute(cmdLine, resultHandler);

		resultHandler.waitFor();

	}

}
