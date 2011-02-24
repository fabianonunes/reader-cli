/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.fabianonunes.reader.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;

public class DefaultExecutorTest {

	public static void main(String[] args) throws ExecuteException, IOException, InterruptedException {

		File pdfFile = new File("/home/fabiano/workdir/converter/inicial.pdf");

		RandomAccessFileOrArray raf = new RandomAccessFileOrArray(
				pdfFile.getAbsolutePath());

		PdfReader reader = new PdfReader(raf, null);

		int numOfPages = reader.getNumberOfPages();

		reader.close();

		Integer iterations = new Double(Math.ceil(numOfPages / 8f)).intValue();

		Integer step = 8;

		File outputDir = new File("/home/fabiano/workdir/converter/output");

		System.out.println("Converting pdf to images...");
		
		
		ArrayList<DefaultExecuteResultHandler> handlers = new ArrayList<DefaultExecuteResultHandler>();
		
		
		for (int i = 0; i < iterations; i++) {

			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			
			handlers.add(resultHandler);
			
			int firstPage = step * i + 1;
			int lastPage = Math.min(numOfPages, (firstPage + step - 1));

			String command = "pdftoppm -r 300" + //
					" -f " + firstPage + //
					" -l " + lastPage + //
					" -gray -scale-to 1000 " + //
					pdfFile.getAbsolutePath() + //
					" " + outputDir.getAbsolutePath() + "/p";
			
			CommandLine cmdLine = CommandLine.parse(command);

			ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
			Executor exec = new DefaultExecutor();
			exec.setWatchdog(watchdog);
			exec.setExitValue(0);
			
			exec.execute(cmdLine, resultHandler);
			

		}
		
		System.out.println("OK");
		
		for (DefaultExecuteResultHandler handler : handlers) {
			
			handler.waitFor();
			
		}
		
		System.out.println("Finished");

	}

}