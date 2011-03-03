package com.fabianonunes.reader.text.analysys;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

public class XMLPositionTokenizer extends Tokenizer {

	private OffsetAttribute offsetAtt;
	private TermAttribute termAtt;
	private PayloadAttribute payloadAtt;
	private Document doc;

	private int offset = 0, dataLen = 0;
	private List<Element> words;

	private float pageWidth;
	private float pageHeight;

	@SuppressWarnings("unchecked")
	public XMLPositionTokenizer(Reader in) throws JDOMException, IOException {

		super(CharReader.get(in));

		offsetAtt = addAttribute(OffsetAttribute.class);
		termAtt = addAttribute(TermAttribute.class);
		payloadAtt = addAttribute(PayloadAttribute.class);

		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(false);
		builder.setIgnoringElementContentWhitespace(true);

		doc = builder.build(in);

		pageWidth = Float.parseFloat(doc.getRootElement().getAttributeValue(
				"width"));

		pageHeight = Float.parseFloat(doc.getRootElement().getAttributeValue(
				"height"));

		XPath xpath = XPath.newInstance("//TOKEN");

		words = xpath.selectNodes(doc);

	}

	@Override
	public boolean incrementToken() throws IOException {

		clearAttributes();

		int current = offset++;

		if (words.size() <= current) {
			return false;
		}

		Element word = words.get(current);

		String text = word.getText().trim();

		int textLength = text.length();

		termAtt.setTermBuffer(text);

		termAtt.setTermLength(textLength);

		// offsetAtt.setOffset(correctOffset(dataLen), dataLen + textLength);
		offsetAtt.setOffset(dataLen, dataLen + textLength);

		float left = Float.parseFloat(word.getAttributeValue("x"));
		float top = Float.parseFloat(word.getAttributeValue("y"));
		float width = Float.parseFloat(word.getAttributeValue("width"));
		float height = Float.parseFloat(word.getAttributeValue("height"));

		int ileft = Math.round(left * 1000 / pageWidth);
		int itop = Math.round(top * 1000 / pageHeight);
		int iwidth = Math.round(width * 1000 / pageWidth);
		int iheight = Math.round(height * 1000 / pageHeight);

		// x,y,w,h
		String coords = ileft + "," + itop + "," + iwidth + "," + iheight;

		Payload payload = new Payload(coords.getBytes());
		payloadAtt.setPayload(payload);

		dataLen += textLength + 1;

		return true;

	}
}
