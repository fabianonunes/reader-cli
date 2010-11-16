package com.fabianonunes.reader.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

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

public class XmlAssembler {

	public static void assemble(File[] files, File output, String query)
			throws XPathParseException, IOException, NavException,
			XPathEvalException, EncodingException, EOFException,
			EntityException, ParseException {

		VTDGen vg;

		FileUtils.deleteQuietly(output);

		FileOutputStream fos = new FileOutputStream(output);

		AutoPilot ap = new AutoPilot();

		FastLongBuffer flb = new FastLongBuffer(4);

		ap.selectXPath(query);

		fos.write("<DOCUMENT>\n".getBytes());

		for (File file : files) {

			System.out.println(file);

			byte[] b = FileUtils.readFileToByteArray(file);

			vg = new VTDGen();
			vg.setDoc(b);
			vg.parse(false);

			VTDNav nav = vg.getNav();

			ap.bind(nav);

			byte[] xml = nav.getXML().getBytes();

			while (ap.evalXPath() != -1) {
				flb.append(nav.getElementFragment());
			}

			int size = flb.size();

			for (int k = 0; k < size; k++) {

				fos.write("\n".getBytes());

				fos.write(xml, flb.lower32At(k), flb.upper32At(k));

			}

			ap.resetXPath();

			flb.clear();

		}

		fos.write("\n</DOCUMENT>".getBytes());

		fos.close();

		for (File file : files) {

			FileUtils.deleteQuietly(file);

		}

	}

}
