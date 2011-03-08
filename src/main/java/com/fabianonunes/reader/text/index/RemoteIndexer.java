package com.fabianonunes.reader.text.index;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

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

public class RemoteIndexer {

	private SolrServer server;
	private String documentName;

	public RemoteIndexer(String documentName) throws MalformedURLException {

		this.documentName = documentName;

		server = new CommonsHttpSolrServer("http://localhost:8081/solr");

	}

	public void indexXMLFile(File xmlFile) throws EncodingException,
			EOFException, EntityException, ParseException, IOException,
			XPathParseException, NavException, XPathEvalException,
			TranscodeException, SolrServerException {

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

		server.commit();

	}

	private void indexPage(String contents, String plaintext, String pageNumber)
			throws SolrServerException, IOException {

		SolrInputDocument document = new SolrInputDocument();
		document.addField("content", contents);
		document.addField("plaintext", plaintext);
		document.addField("page", pageNumber);
		document.addField("name", documentName);

		server.add(document);

	}
}
