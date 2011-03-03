package com.fabianonunes.reader.text.analysys;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;

import com.ximpleware.AutoPilot;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathParseException;

public class VtdPositionTokenizer extends Tokenizer {

	private OffsetAttribute offsetAtt;
	private TermAttribute termAtt;
	private PayloadAttribute payloadAtt;

	private int dataLen = 0;
	private VTDGen vg;
	private VTDNav nav;
	private AutoPilot ap;

	public VtdPositionTokenizer(Reader in) throws IOException,
			EncodingException, EOFException, EntityException, ParseException,
			XPathParseException, NavException {

		super(in);

		offsetAtt = addAttribute(OffsetAttribute.class);
		termAtt = addAttribute(TermAttribute.class);
		payloadAtt = addAttribute(PayloadAttribute.class);

		vg = new VTDGen();
		vg.setDoc(IOUtils.toByteArray(in));
		vg.parse(false);

		nav = vg.getNav();

		ap = new AutoPilot(nav);
		ap.selectXPath("//w");

	}

	@Override
	public boolean incrementToken() throws IOException {

		int t;

		clearAttributes();

		try {

			if (ap.evalXPath() == -1) {
				return false;
			}

			VTDNav vn = nav.cloneNav();

			String text = "";

			t = vn.getText();

			if (t > -1) {

				text = vn.toNormalizedString(t).trim();

			}

			int textLength = text.length();

			termAtt.setTermBuffer(text);

			termAtt.setTermLength(textLength);

			offsetAtt.setOffset(dataLen, dataLen + textLength);

			t = vn.getAttrVal("c");
			String coords = vn.toNormalizedString(t);

			vn.toElement(VTDNav.PARENT);
			t = vn.getAttrVal("n");
			String page = vn.toNormalizedString(t);

			coords = page + "," + coords + "," + text.replaceAll(",", "");
			// + "," + dataLen;

			Payload payload = new Payload(coords.getBytes());
			payloadAtt.setPayload(payload);

			dataLen += textLength + 1;

		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}

		return true;

	}
}
