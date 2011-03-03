package com.fabianonunes.reader.text.index;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import com.fabianonunes.reader.text.analysys.VtdPositionPayloadAnalyzer;
import com.ximpleware.AutoPilot;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.FastLongBuffer;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class Indexer {

	protected File folder;
	protected Analyzer ppa;
	private IndexWriter indexWriter;
	private Directory d = new RAMDirectory();
	private String docName;

	public Indexer(File folder, String docName) throws IOException {

		ppa = new VtdPositionPayloadAnalyzer();

		d = FSDirectory.open(folder);

		this.docName = docName;

		indexWriter = new IndexWriter(d, ppa, true, MaxFieldLength.UNLIMITED);

	}

	public void indexXMLFile(File xmlFile) throws EncodingException,
			EOFException, EntityException, ParseException, IOException,
			XPathParseException, NavException, XPathEvalException,
			JDOMException {

		VTDGen vg;

		AutoPilot ap = new AutoPilot();
		ap.selectXPath("//page");

		byte[] b = FileUtils.readFileToByteArray(xmlFile);
		vg = new VTDGen();
		vg.setDoc(b);
		vg.parse(false);

		VTDNav nav = vg.getNav();
		ap.bind(nav);

		byte[] xml = nav.getXML().getBytes();

		int count = 0, t;
		while (ap.evalXPath() != -1) {

			FastLongBuffer flb = new FastLongBuffer(4);

			ByteArrayOutputStream fos = new ByteArrayOutputStream();

			flb.append(nav.getElementFragment());

			int size = flb.size();

			for (int k = 0; k < size; k++) {

				fos.write("\n".getBytes());

				fos.write(xml, flb.lower32At(k), flb.upper32At(k));

			}

			t = nav.getAttrVal("n");
			String pageNumber = nav.toNormalizedString(t);

			indexPage(fos.toString(), pageNumber);

			count++;

		}

		ap.resetXPath();

	}

	private void indexPage(String contents, String pageNumber)
			throws IOException, JDOMException {

		Document document = new Document();

		String plaintext = getText(contents);

		// TokenStream tokenStream = TokenSources.getTokenStream("content",
		// contents, ppa);
		// Field contentField = new Field("plaintext", plaintext,
		// Field.Store.YES,
		// Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
		// contentField.setTokenStream(tokenStream);
		// document.add(contentField);

		document.add(new Field("content", contents, Field.Store.NO,
				Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));

		document.add(new Field("plaintext", plaintext, Field.Store.YES,
				Field.Index.NO));

		document.add(new Field("page", pageNumber, Field.Store.YES,
				Field.Index.ANALYZED));

		document.add(new Field("name", docName, Field.Store.YES,
				Field.Index.NOT_ANALYZED));

		indexWriter.addDocument(document);

	}

	private String getText(String text) throws JDOMException, IOException {

		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(false);
		builder.setIgnoringElementContentWhitespace(true);
		org.jdom.Document doc = builder.build(new StringReader(text));

		XPath xpath = XPath.newInstance("//page");

		Element page = (Element) xpath.selectSingleNode(doc);

		return page.getValue().replaceAll("\\s+", " ").trim();

	}

	public void close() throws CorruptIndexException, IOException {
		indexWriter.optimize();
		indexWriter.close();
	}

}
