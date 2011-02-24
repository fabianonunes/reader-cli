package com.fabianonunes.reader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
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
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.fabianonunes.reader.pdf.outline.OutlineHandler;
import com.fabianonunes.reader.pdf.text.position.OptiXML;
import com.fabianonunes.reader.pdf.text.position.SimpleXML;
import com.fabianonunes.reader.storage.ReaderDocument;
import com.fabianonunes.reader.text.classification.Classifier;
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

		if (metodo.equals("simple-xml")) {

			simpleXML(input);

		} else if (metodo.equals("merge-xml")) {

			mergeXML(input);

		} else if (metodo.equals("optimized-xml")) {

			optimizeXML(input);

		} else {

			ReaderDocument doc = new ReaderDocument(input);

			if (metodo.equals("build-index")) {

				doc.buildIndex();

				FileFilter filter = FileFilterUtils.suffixFileFilter(".xml");

				File[] rules = rulesFolder.listFiles(filter);

				Classifier c = new Classifier(doc.getIndexFolder());

				TreeMap<String, List<Integer>> results = c.analyze(rules);

				OutlineHandler outline = new OutlineHandler(results);

				JSONObject data = new JSONObject();

				data.put("children", outline.getRoot());

				doc.saveData(data.toString());

			} else if (metodo.equals("auto-bookmark")) {

			} else if (metodo.equals("extract-outline")) {

				doc.extractData();

			}

		}

	}

	private static void mergeXML(File input) throws EncodingException,
			EOFException, EntityException, XPathParseException, NavException,
			XPathEvalException, com.ximpleware.ParseException, IOException {

		XmlAssembler.assemble(input.listFiles(), new File(input, "full.xml"),
				"//PAGE");

	}

	public static void simpleXML(File file) throws Exception {

		SimpleXML xmlt = new SimpleXML(file);

		xmlt.convert();

		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

		FileWriter w = new FileWriter(
				new File(file.getParentFile(), "text.xml").getAbsolutePath());

		outputter.output(xmlt.getDoc(), w);

		xmlt.resetDocument();

		xmlt = null;

	}

	public static void optimizeXML(File file) throws EncodingException,
			EOFException, EntityException, NumberFormatException,
			com.ximpleware.ParseException, XPathParseException,
			XPathEvalException, NavException, IOException, ModifyException,
			TranscodeException {

		OptiXML opti = new OptiXML(file);

		opti.optimize();

		opti = null;

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
						"[simple-xml, " + "optimized-xml, build-index, "
								+ "extract-outline, auto-bookmark, merge-xml]")
				.isRequired().hasArg().create("metodo");

		Option input = OptionBuilder.withArgName("input").hasArg().isRequired()
				.hasArg().create("i");

		Options options = new Options();
		options.addOption(input);
		options.addOption(metodo);
		options.addOption("debug", false, "debug");

		return options;
	}

	public void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("reader-cli", options);
		System.exit(0);
	}

}
