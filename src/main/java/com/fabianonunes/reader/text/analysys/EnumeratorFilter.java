package com.fabianonunes.reader.text.analysys;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.Payload;

public class EnumeratorFilter extends TokenFilter {

	private PayloadAttribute payloadAttr;
	private PositionIncrementAttribute posIncrAttr;
	private int index;

	public EnumeratorFilter(TokenStream in) {
		super(in);
		payloadAttr = addAttribute(PayloadAttribute.class);
		posIncrAttr = addAttribute(PositionIncrementAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {

		if (input.incrementToken()) {

			index += posIncrAttr.getPositionIncrement();

			Payload pl = payloadAttr.getPayload();

			String data = new String(pl.getData());

			data += "," + index;

			pl.setData(data.getBytes());

			return true;

		} else {

			return false;

		}
	}

}
