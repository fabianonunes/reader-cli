package com.fabianonunes.reader.storage;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.TreeSet;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.fabianonunes.reader.cli.pdf.PdfHandler;

public class ReaderDocument implements Serializable {

	private static final long serialVersionUID = 1L;

	private File folder;
	private File outlineFolder;
	private File indexFolder;
	private File imageFolder;
	private File thumbsFolder;
	private File simpleTextFolder;

	public ReaderDocument(File folder) throws IOException {

		if (folder.isDirectory()) {

			this.folder = folder;

			createFolders();

		} else {

			throw new FileNotFoundException("File not found: "
					+ folder.getName());

		}

	}

	private void createFolders() throws IOException {

		simpleTextFolder = new File(getFolder(), "text/simple");
		if (!simpleTextFolder.exists()) {
			FileUtils.forceMkdir(simpleTextFolder);
		}

		imageFolder = new File(getFolder(), "images/png");
		if (!imageFolder.exists()) {
			FileUtils.forceMkdir(imageFolder);
		}

		thumbsFolder = new File(imageFolder, "t");
		if (!thumbsFolder.exists()) {
			FileUtils.forceMkdir(thumbsFolder);
		}

		indexFolder = new File(getFolder(), "index");
		if (!indexFolder.exists()) {
			indexFolder.mkdir();
		}

		outlineFolder = new File(getFolder(), "outline");
		if (!outlineFolder.exists()) {
			outlineFolder.mkdir();
		}

	}

	public File getPdf() {

		FileFilter pdf = FileFilterUtils.suffixFileFilter(".pdf");

		File[] files = folder.listFiles(pdf);

		if (files.length > 0) {

			File r = files[0];

			if (r.exists()) {
				return r;
			}

		}

		return null;

	}

	public File getImage(int pageNumber) {

		File img = new File(getFolder(), "images/png/" + pageNumber + ".png");

		if (img.exists()) {

			return img;
		}

		return null;

	}

	public File getText() {

		File xml = new File(getFolder(), "text/text.xml");

		if (xml.exists()) {

			return xml;
		}

		return null;

	}

	public File getFullText() {

		File xml = new File(getFolder(), "text/full.xml");

		return xml;

	}

	public File getOptiText() {

		File xml = new File(getFolder(), "text/opti.xml");

		if (xml.exists()) {

			return xml;
		}

		return null;

	}

	public void saveData(String jsonOutline) throws IOException {

		long time = System.currentTimeMillis();

		File outline = new File(outlineFolder, time + ".json");

		FileUtils.deleteQuietly(outline);

		FileUtils.writeStringToFile(outline, jsonOutline);

	}

	public File getLatestOutline() {

		String[] outlines = outlineFolder.list();

		TreeSet<String> os = new TreeSet<String>(Arrays.asList(outlines));

		if (os.size() > 0) {

			File last = new File(outlineFolder, os.last());

			if (last.exists()) {

				return last;

			}

		}

		return null;

	}

	public Integer getNumberOfLeafs() {

		String[] files = getThumbsFolder().list();

		return files.length;

	}

	// TODO: remover esse método daqui
	public Boolean extractData() throws IOException {

		File pdf = getPdf();

		if (pdf == null) {
			return false;
		}

		PdfHandler handler = null;

		try {

			handler = new PdfHandler(pdf);

			JSONObject data = new JSONObject();

			data.put("children", handler.getOutlineAsJSON());

			String outline = data.toString();

			saveData(outline);

		} catch (IOException e) {

			throw e;

		} finally {

			if (handler != null) {

				handler.close();

			}

		}

		return true;

	}

	public File getThumbImage(Integer pageNumber) {

		File img = new File(getFolder(), "images/png/t/" + pageNumber + ".png");

		if (img.exists()) {

			return img;
		}

		return null;
	}

	public File getFolder() {
		return folder;
	}

	public File getOutlineFolder() {
		return outlineFolder;
	}

	public File getImageFolder() {
		return imageFolder;
	}

	public File getThumbsFolder() {
		return thumbsFolder;
	}

	public File getTextFolder() {
		return simpleTextFolder.getParentFile();
	}

	public File getSimpleTextFolder() {
		return simpleTextFolder;
	}

	public File getIndexFolder() {
		return indexFolder;
	}

	public static ReaderDocument generateDocument(File pdfFile)
			throws IOException {

		String fileName = pdfFile.getAbsolutePath();

		fileName = FilenameUtils.getBaseName(fileName);

		File documentFolder = new File(pdfFile.getParentFile(), fileName);

		FileUtils.deleteQuietly(documentFolder);

		File documentFile = new File(documentFolder, pdfFile.getName());

		FileUtils.moveFile(pdfFile, documentFile);

		return new ReaderDocument(documentFolder);

	}

}
