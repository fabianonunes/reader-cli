package com.fabianonunes.reader.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.tidy.Tidy;

import com.ximpleware.AutoPilot;
import com.ximpleware.ModifyException;
import com.ximpleware.NavException;
import com.ximpleware.TranscodeException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class Modifier {

	private VTDNav nav;
	private AutoPilot ap;
	private XMLModifier xm;

	public static void main(String[] args) throws ModifyException,
			NavException, TranscodeException, FileNotFoundException,
			XPathParseException, XPathEvalException, IOException,
			TransformerFactoryConfigurationError, TransformerException {
		Modifier m = new Modifier();
		m.exec();
	}

	public void exec() throws ModifyException, NavException,
			TranscodeException, FileNotFoundException, IOException,
			XPathParseException, XPathEvalException,
			TransformerFactoryConfigurationError, TransformerException {

		VTDGen vg = new VTDGen(); // Instantiate VTDGen

		xm = new XMLModifier(); // Instantiate XMLModifier

		if (vg.parseFile("/home/fabiano/workdir/full.xml", false)) {

			nav = vg.getNav();

			xm.bind(nav);

			xm.updateElementName("full");

			ap = new AutoPilot(nav);

			ap.selectXPath("//PAGE");

			int t;

			while (ap.evalXPath() != -1) {

				// VTDNav vn = nav.cloneNav();

				xm.updateElementName("page");

				t = nav.getAttrVal("width");
				float pageWidth = nav.parseFloat(t);
				removeAttr("width");

				t = nav.getAttrVal("height");
				float pageHeight = nav.parseFloat(t);
				removeAttr("height");

				removeAttr("id");

				ap.selectAttr("number");
				t = ap.iterateAttr();

				xm.updateToken(t, "n");

				t = nav.getAttrVal("number");

				if (t == -1)
					continue;

				if (!nav.toElement(VTDNav.FIRST_CHILD)) {
					continue;
				}

				boolean hasNext = true;

				while (!nav.matchElement("TEXT") && hasNext) {
					hasNext = nav.toElement(VTDNav.NEXT_SIBLING);
				}

				if (!hasNext) {
					continue;
				}

				do {

					// VTDNav ivn = vn.cloneNav();

					if (nav.toElement(VTDNav.FIRST_CHILD)) {

						StringBuffer data = new StringBuffer();

						do {

							// Element newWord = new Element("w");

							t = nav.getAttrVal("x");
							float left = nav.parseFloat(t);

							t = nav.getAttrVal("y");
							float top = nav.parseFloat(t);

							t = nav.getAttrVal("width");
							float width = nav.parseFloat(t);

							t = nav.getAttrVal("height");
							float height = nav.parseFloat(t);

							int iX = Math.round(left * 1000 / pageWidth);
							int iY = Math.round(top * 1000 / pageHeight);
							int iW = Math.round(width * 1000 / pageWidth);
							int iH = Math.round(height * 1000 / pageHeight);

							// x,y,w,h
							String coords = iX + "," + iY + "," + iW + "," + iH;

							t = nav.getText();
							String tokenText = nav.toNormalizedString(t);

							tokenText = StringEscapeUtils.escapeXml(tokenText);

							data.append("<w c=\"" + coords + "\">" + tokenText
									+ "</w>\n\t");

						} while (nav.toElement(VTDNav.NEXT_SIBLING));

						nav.toElement(VTDNav.PARENT);

						xm.insertAfterElement(data.toString().getBytes());

					}

					xm.remove();

				} while (nav.toElement(VTDNav.NEXT_SIBLING));

				nav.toElement(VTDNav.PARENT);

			}

			ap.resetXPath();

			vg.clear();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			xm.output(baos);

			// XMLSerializer s = new XMLSerializer();
			//
			// Transformer tr =
			// TransformerFactory.newInstance().newTransformer();
			//
			// tr.setOutputProperty(OutputKeys.INDENT, "yes");

			ByteArrayInputStream is = new ByteArrayInputStream(
					baos.toByteArray());
			
			Tidy tidy = new Tidy();
			
			tidy.setXmlTags(true);
			tidy.setXmlOut(true);
			tidy.setWraplen(255);
			tidy.setMakeClean(true);
			tidy.setInputEncoding("utf-8");
			tidy.setIndentContent(true);
			tidy.setQuiet(true);
			tidy.setForceOutput(true);
			
			tidy.parse(is, System.out);
			
			
			//-utf8 -xml -w 255 -i -c -q -asxml -o "+ output.getAbsolutePath() + " " + output.getAbsolutePath();
			

			// tr.transform(new StreamSource(is), new StreamResult(System.out));

			// s.re

		}

	}

	private void removeAttr(String attr) throws NavException, ModifyException {

		ap.selectAttr(attr);
		int t = ap.iterateAttr();
		xm.removeAttribute(t);

	}

}
