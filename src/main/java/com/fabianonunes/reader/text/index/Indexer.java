package com.fabianonunes.reader.text.index;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import com.fabianonunes.reader.text.analysys.VtdPositionPayloadAnalyzer;
import com.ximpleware.AutoPilot;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.FastLongBuffer;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.TranscodeException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class Indexer {

	protected File folder;
	protected Analyzer ppa;
	private IndexWriter indexWriter;
	private Directory directory;
	private String docName;

	public Indexer(File folder, String docName) throws IOException {

		this(FSDirectory.open(folder), docName);

	}

	public Indexer(Directory d, String docName) throws CorruptIndexException,
			LockObtainFailedException, IOException {

		ppa = new VtdPositionPayloadAnalyzer();

		this.directory = d;

		this.docName = docName;

		indexWriter = new IndexWriter(this.directory, ppa, true,
				MaxFieldLength.UNLIMITED);

	}

	public void indexXMLFile(File xmlFile) throws EncodingException,
			EOFException, EntityException, ParseException, IOException,
			XPathParseException, NavException, XPathEvalException,
			TranscodeException {

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

			fos.close();
			
			VTDNav n = nav.cloneNav();

			t = n.getAttrVal("n");

			String pageNumber = n.toNormalizedString(t);

			boolean hasChild = n.toElement(VTDNav.FIRST_CHILD);

			if (!hasChild) {
				continue;
			}

			StringBuffer buffer = new StringBuffer();

			while (n.toElement(VTDNav.NEXT_SIBLING)) {

				t = n.getText();

				if (t > -1) {

					buffer.append(n.toNormalizedString(t).trim() + " ");

				}

			}

			indexPage(fos.toString("utf-8"), buffer.toString().trim(),
					pageNumber);

			count++;

		}

		ap.resetXPath();

	}

	private void indexPage(String contents, String plaintext, String pageNumber)
			throws IOException {

		Document document = new Document();

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

	public void close() throws CorruptIndexException, IOException {
		indexWriter.commit();
		indexWriter.optimize();
		indexWriter.close();
	}
	
	public IndexWriter getIndexWriter(){
		return indexWriter;
	}

}
