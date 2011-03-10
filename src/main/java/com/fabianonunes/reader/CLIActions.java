package com.fabianonunes.reader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import net.sf.json.JSONObject;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jdom.JDOMException;

import com.fabianonunes.reader.cli.Converter;
import com.fabianonunes.reader.pdf.outline.OutlineHandler;
import com.fabianonunes.reader.storage.ReaderDocument;
import com.fabianonunes.reader.text.classification.Classifier;
import com.fabianonunes.reader.text.position.OptiXML;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.ModifyException;
import com.ximpleware.NavException;
import com.ximpleware.TranscodeException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class CLIActions {

	private static File rulesFolder = new File(
			"/home/fabiano/workdir/reader-tests/rules");

	public static void main(String[] args) throws Throwable {

		CLIActions cli = new CLIActions();

		CommandLine line = cli.parseCommandLine(args);

		File input = new File(line.getOptionValue("i"));

		String metodo = line.getOptionValue("metodo");

		ReaderDocument doc = new ReaderDocument(input);

		Converter c = new Converter(doc);

		if (metodo.equals("build-index")) {

			c.indexDocument(line.getOptionValue("solrHome"));

		} else if (metodo.equals("build-lucene-index")) {

			c.indexDocument();

		} else if (metodo.equals("extract-text")) {

			c.extractText();

		} else if (metodo.equals("extract-images")) {

			c.extractImages();

		} else if (metodo.equals("optimize-text")) {

			c.manipulateTextFiles();

		} else if (metodo.equals("optimize-images")) {

			c.optimizeImages();

		} else if (metodo.equals("store-db")) {

			c.storeToDB();

		} else if (metodo.equals("auto-bookmark")) {

		} else if (metodo.equals("extract-data")) {

			c.extractPdfData();

		}

	}

	@Deprecated
	public static void buildIndex(ReaderDocument doc) throws EncodingException,
			EOFException, EntityException, com.ximpleware.ParseException,
			XPathParseException, NavException, XPathEvalException, IOException,
			org.apache.lucene.queryParser.ParseException, JDOMException {

		// doc.buildIndex();

		FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");

		File[] rules = rulesFolder.listFiles(filter);

		Classifier c = new Classifier(doc.getIndexFolder());

		TreeMap<String, List<Integer>> results = c.analyze(rules);

		OutlineHandler outline = new OutlineHandler(results);

		JSONObject data = new JSONObject();

		data.put("children", outline.getRoot());

		doc.saveData(data.toString());

	}

	public static void optimizeXML(File file) throws EncodingException,
			EOFException, EntityException, NumberFormatException,
			com.ximpleware.ParseException, XPathParseException,
			XPathEvalException, NavException, IOException, ModifyException,
			TranscodeException {

		OptiXML opti = new OptiXML(file);

		opti.optimize();

	}

	public CommandLine parseCommandLine(String[] args) {

		Options options = buildOptions();

		CommandLineParser parser = new GnuParser();

		CommandLine line = null;

		try {

			line = parser.parse(options, args);

			if (line.getOptions().length == 0) {
				printHelp(options);
			}

		} catch (ParseException e) {
			printHelp(options);
		}

		return line;

	}

	@SuppressWarnings("static-access")
	private Options buildOptions() {

		Option metodo = OptionBuilder
				.withArgName("metodo")
				.withDescription(
						"[xml, build-index, "
								+ "extract-outline, auto-bookmark]")
				.isRequired().hasArg().create("metodo");

		Option input = OptionBuilder.withArgName("input").hasArg().isRequired()
				.hasArg().create("i");

		Options options = new Options();
		options.addOption(input);
		options.addOption(metodo);
		options.addOption(OptionBuilder.withArgName("solrHome").hasArg()
				.create("solrHome"));
		options.addOption("debug", false, "debug");

		return options;
	}

	public void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("reader-cli", options);
		System.exit(0);
	}

}
