package com.fabianonunes.reader.text.classification.rules;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

public class ClassifierRule {

	private String name;
	private Map<String, String> variables;
	private String queryString;
	private Document document;
	private XPath xpath;

	public ClassifierRule(File rule) throws JDOMException, IOException {

		this(FileUtils.readFileToString(rule));

	}

	public ClassifierRule(String contents) throws JDOMException, IOException {

		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(false);
		builder.setIgnoringElementContentWhitespace(true);

		Pattern p = Pattern.compile("\\{[^\\}]*\\}", Pattern.MULTILINE);

		contents = p.matcher(contents).replaceAll("");
		
		document = builder.build(new StringReader(contents));
		
	}

	public String getName() throws JDOMException {

		if (name == null) {

			String query = "//name[1]";

			xpath = XPath.newInstance(query);

			name = xpath.valueOf(document);
		}

		return name;

	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getVariables() throws JDOMException {

		if (variables == null) {

			variables = new TreeMap<String, String>();

			String query = "//var";

			xpath = XPath.newInstance(query);

			List<Element> nodes = xpath.selectNodes(document);

			for (Element element : nodes) {

				variables.put(element.getAttributeValue("name"), element
						.getText().replaceAll("\\s+", " "));

			}
			
		}

		return variables;
	}

	public String queryString() throws JDOMException {

		String query = "//query[1]";

		xpath = XPath.newInstance(query);

		queryString = xpath.valueOf(document);

		Map<String, String> vars = getVariables();

		for (String variable : vars.keySet()) {

			queryString = queryString.replaceAll("\\[" + variable + "\\]", vars
					.get(variable).trim());

		}

		return queryString.replaceAll("\\s+", " ");

	}
}
