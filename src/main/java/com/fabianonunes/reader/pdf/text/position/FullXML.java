package com.fabianonunes.reader.pdf.text.position;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import com.ximpleware.AutoPilot;
import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class FullXML {

	private VTDGen vg;
	private VTDNav nav;

	public FullXML(File file) throws IOException, EncodingException,
			EOFException, EntityException, ParseException {

		byte[] b = FileUtils.readFileToByteArray(file);

		vg = new VTDGen();
		vg.setDoc(b);
		vg.parse(false);

		nav = vg.getNav();

	}

	public Map<Integer, StringBuffer> getTextOfPages(double pageCoverage)
			throws XPathParseException, NumberFormatException,
			XPathEvalException, NavException {

		Map<Integer, StringBuffer> text = new TreeMap<Integer, StringBuffer>();

		String query = "//PAGE//TEXT";

		if (pageCoverage < 1) {

			query = "//PAGE/@height";

			AutoPilot ap = new AutoPilot(nav);

			ap.selectXPath(query);

			float height = Float.parseFloat(ap.evalXPathToString());

			long coverage = Math.round(height * pageCoverage);

			query = "//PAGE//TEXT[@y < " + coverage + "]";

		}

		AutoPilot ap = new AutoPilot(nav);

		ap.selectXPath(query);

		while (ap.evalXPath() != -1) {

			VTDNav vn;

			int t;

			String lineText = "";

			vn = nav.cloneNav();
			vn.toElement(VTDNav.PARENT);
			t = vn.getAttrVal("number");
			Integer pageNumber = Integer.parseInt(vn.toNormalizedString(t));

			if (!text.containsKey(pageNumber)) {
				text.put(pageNumber, new StringBuffer());
			}

			vn = nav.cloneNav();

			if (vn.toElement(VTDNav.FIRST_CHILD)) {

				do {

					t = vn.getText();

					lineText += vn.toNormalizedString(t) + " ";

				} while (vn.toElement(4));

				text.get(pageNumber).append(cleanString(lineText) + " ");

			}

		}

		ap.resetXPath();

		return text;

	}

	public Map<Integer, StringBuffer> getTextOfPages()
			throws NumberFormatException, XPathParseException,
			XPathEvalException, NavException {
		return getTextOfPages(1);

	}

	public List<String> evaluateXPath(String query,
			XPathEvaluator<String> evaluator) throws XPathEvalException,
			NavException, Exception {

		List<String> retVal = new ArrayList<String>();

		AutoPilot ap = new AutoPilot(nav);

		ap.selectXPath(query);

		while (ap.evalXPath() != -1) {

			retVal.add(evaluator.call(nav, this));

		}

		return retVal;

	}

	public interface XPathEvaluator<T> {

		public T call(VTDNav nav, FullXML fullXML) throws Exception;

	}

	public String cleanString(String lineText) {

		String contents = lineText;

		contents = contents
				.replaceAll(
						"([\\w\u00C0-\u00FF])[<>!\\?\\^'\"~\\(\\)]([\\w\u00C0-\u00FF])",
						"$1$2");

		contents = contents.replaceAll(
				"[<>:,\\.!\\?\\^'\"~_\\(\\)\\{\\}\\[\\]]", " ");

		contents = contents.replaceAll("\\s+", " ");

		return contents.trim();

	}

	public Integer getNumOfPages() throws Exception {

		String query = "//PAGE[last()]/@number";

		AutoPilot ap = new AutoPilot(nav);

		ap.selectXPath(query);

		return Integer.parseInt(ap.evalXPathToString());

	}

	public void close() {

		vg.clear();
		vg = null;
		nav = null;

	}

}
