package com.fabianonunes.reader.pdf.stripper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

public class PageStripper {

	private File pdfFile;
	private File outPut;

	public PageStripper(File pdfFile, File outputFile) {

		this.pdfFile = pdfFile;
		this.outPut = outputFile;

	}

	public void extractPages(List<Integer> pageList) throws IOException,
			DocumentException {

		PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());

		reader.selectPages(pageList);

		int pages = reader.getNumberOfPages();

		Document document = new Document();

		PdfCopy copy = new PdfCopy(document, new FileOutputStream(outPut));

		document.open();

		for (int i = 0; i < pages;) {
			++i;
			copy.addPage(copy.getImportedPage(reader, i));
		}
		
		document.close();

	}

}
