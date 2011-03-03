package com.fabianonunes.reader.pdf.text.position;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public abstract class AbstractTextPosition implements TextPosition {

	private Document doc;

	private File file;

	private Map<Integer, Element> pages;

	public AbstractTextPosition(File file) {

		setFile(file);

		resetDocument();

	}

	public void resetDocument() {

		Element root = new Element("pdfxml");

		doc = new Document(root);

		pages = new TreeMap<Integer, Element>();

	}

	public Element addPage(Number height, Number width, Number pageNo) {

		Element page = new Element("page");

		page.setAttribute("h", Integer.toString(height.intValue()));
		page.setAttribute("w", Integer.toString(width.intValue()));
		page.setAttribute("n", Integer.toString(pageNo.intValue()));

		doc.getRootElement().addContent(page);

		pages.put(pageNo.intValue(), page);

		return page;

	}

	public void addTextLine(Integer pageNo, Number height, Number width,
			Number top, Number left, List<Integer> wds, String text)
			throws JDOMException {

		Element token = new Element("tk");

		token.setAttribute("h", Integer.toString(height.intValue()));
		token.setAttribute("w", Integer.toString(width.intValue()));
		token.setAttribute("l", Integer.toString(left.intValue()));
		token.setAttribute("t", Integer.toString(top.intValue()));

		if (wds != null) {
			String ws = StringUtils.join(wds, ",");
			token.setAttribute("ws", ws);
		}

		token.setText(text);

		// XPath xpath = XPath.newInstance("//page[@n='" + pageNo + "']");
		// Element page = (Element) xpath.selectSingleNode(doc);

		Element page = pages.get(pageNo);

		if (page == null) {
			throw new JDOMException("Element not found");
		}

		page.addContent(token);

	}

	public void printDoc() throws IOException {

		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

		IOUtils.write(outputter.outputString(doc), System.out, "utf-8");

	}

	@Override
	public String getXmlDoc(Integer pageNumber) throws Throwable {

		this.calculatePositions(pageNumber);

		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

		StringWriter w = new StringWriter();

		outputter.output(doc, w);

		return w.toString();

	}

	public Document getDoc() {
		return doc;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

}
