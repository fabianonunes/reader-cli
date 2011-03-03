package com.fabianonunes.reader.pdf.text.position;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

public class SimpleXML extends AbstractTextPosition {

	public SimpleXML(File file) {
		super(file);
	}

	public String calculatePositions(Integer pageNumber) throws Throwable {

		String query = "//page[@n='" + pageNumber + "']";

		FileInputStream fis = new FileInputStream(getFile());
		byte[] b = new byte[(int) getFile().length()];
		fis.read(b);
		fis.close();

		VTDGen vg = new VTDGen();
		vg.setDoc(b);
		vg.parse(false);

		VTDNav nav = vg.getNav();

		AutoPilot ap = new AutoPilot(nav);
		ap.selectXPath(query);

		byte[] ba = nav.getXML().getBytes();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write("<pdfxml>".getBytes());

		while (ap.evalXPath() != -1) {

			long l = nav.getElementFragment();
			int offset = (int) l;
			int len = (int) (l >> 32);
			baos.write('\n');
			baos.write(ba, offset, len);

		}

		baos.write("</pdfxml>".getBytes());
		baos.close();

		ap.resetXPath();
		vg.clear();

		String output = new String(baos.toByteArray(), "utf-8");

		return output;

	}

	public void convert() throws Exception {

		resetDocument();

		VTDGen vg = new VTDGen();
		vg.setDoc(FileUtils.readFileToByteArray(getFile()));
		vg.parse(false);

		VTDNav nav = vg.getNav();

		AutoPilot ap = new AutoPilot(nav);

		String query = "//PAGE";

		ap.selectXPath(query);

		int count = 0;

		while (ap.evalXPath() != -1) {

			VTDNav vn = nav.cloneNav();

			int t = vn.getAttrVal("number");

			if (t == -1)
				continue;

			int pageNumber = Integer.parseInt(vn.toNormalizedString(t));

			t = vn.getAttrVal("height");
			String pageHeight = vn.toNormalizedString(t);
			t = vn.getAttrVal("width");
			String pageWidth = vn.toNormalizedString(t);

			addPage(Float.parseFloat(pageHeight), Float.parseFloat(pageWidth),
					pageNumber);

			if (!vn.toElement(2)) {
				continue;
			}

			boolean hasNext = true;

			while (!vn.matchElement("TEXT") && hasNext) {
				hasNext = vn.toElement(4);
			}

			if (!hasNext) {
				continue;
			}

			do {

				t = vn.getAttrVal("width");
				String width = vn.toNormalizedString(t);

				t = vn.getAttrVal("height");
				String height = vn.toNormalizedString(t);

				t = vn.getAttrVal("x");
				String left = vn.toNormalizedString(t);

				t = vn.getAttrVal("y");
				String top = vn.toNormalizedString(t);

				VTDNav ivn = vn.cloneNav();

				List<Integer> ws = new ArrayList<Integer>();
				List<String> text = new ArrayList<String>();

				if (ivn.toElement(2)) {

					do {

						t = ivn.getAttrVal("x");
						String attX = ivn.toNormalizedString(t);

						t = ivn.getAttrVal("width");
						String attWidth = ivn.toNormalizedString(t);

						t = ivn.getText();
						String tokenText = ivn.toNormalizedString(t);

						int wleft = Math.round(Float.parseFloat(attX));
						int wwidth = Math.round(Float.parseFloat(attWidth));
						ws.add(wleft);
						ws.add(wwidth);

						text.add(tokenText);

					} while (ivn.toElement(4));

				}

				addTextLine(pageNumber, Float.parseFloat(height),
						Float.parseFloat(width), Float.parseFloat(top),
						Float.parseFloat(left), ws, StringUtils.join(text, " "));

			} while (vn.toElement(4));

			count++;

		}

		ap.resetXPath();

		vg.clear();
		
	}
}
