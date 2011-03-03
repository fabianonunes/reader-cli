package com.fabianonunes.reader.cli.pdf;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDGamma;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.jdom.JDOMException;

public class PdfHandler {

	private File pdfFile;

	public void setPdfFile(File file) {
		this.pdfFile = file;
	}

	public File getPdfFile() {
		return pdfFile;
	}

	private PDDocument pddoc;

	public PdfHandler() {
	}

	public PdfHandler(File pdfFile) throws IOException {
		setPdfFile(pdfFile);
		pddoc = PDDocument.load(getPdfFile());
	}

	/**
	 * Método acessório responsável pela conversão de um {@link JSONObject} em
	 * um {@link PDOutlineItem}
	 * 
	 * @param jobj
	 * @return Um {@link PDOutlineItem} com todas as propriedades relevantes de
	 *         item
	 */
	@SuppressWarnings("rawtypes")
	private PDOutlineItem jsonToItem(JSONObject jobj) {

		PDOutlineItem item = new PDOutlineItem();

		item.setTitle(jobj.getString("text"));

		Integer pageIndex = jobj.getInt("pageNumber") - 1;

		if (pageIndex < 1) {
			pageIndex = 1;
		}

		if (jobj.has("cls")) {

			String cls = jobj.getString("cls");

			if (cls.equals("node-blue")) {

				item.setBold(true);
				item.setTextColor(Color.BLUE);

			}

		}

		List pages = getPDDoc().getDocumentCatalog().getAllPages();
		PDPage page = (PDPage) pages.get(pageIndex);

		PDPageFitWidthDestination p = new PDPageFitWidthDestination();
		p.setFitBoundingBox(true);
		p.setPage(page);
		item.setDestination(p);

		JSONArray children = jobj.getJSONArray("children");

		if (children != null) {

			for (int i = 0; i < children.size(); i++) {

				item.appendChild(jsonToItem(children.getJSONObject(i)));

			}

		}

		item.openNode();

		return item;

	}

	public JSONArray getOutlineAsJSON() throws IOException {

		PDDocumentOutline root = getPDDoc().getDocumentCatalog()
				.getDocumentOutline();

		PDOutlineItem item = root.getFirstChild();

		JSONArray bookmarks = new JSONArray();

		while (item != null) {

			bookmarks.add(itemToJson(item));

			item = item.getNextSibling();

		}

		return bookmarks;

	}

	/**
	 * O {@link PDDocument} do documento
	 * 
	 * @return
	 */
	public PDDocument getPDDoc() {
		return pddoc;
	}

	/**
	 * Método acessório responsável pela conversão de um {@link PDOutlineItem}
	 * (representaćão de um bookmark no pdfbox) em um {@link JSONObject}
	 * 
	 * @param item
	 * @return Um {@link JSONObject} com todas as propriedades relevantes de
	 *         item
	 * @throws IOException
	 */
	private JSONObject itemToJson(PDOutlineItem item) throws IOException {

		JSONObject itemObj = new JSONObject();

		itemObj.put("text", item.getTitle().trim());
		// itemObj.put("expanded", true);

		PDDestination destination = item.getDestination();

		PDAction action = item.getAction();

		if (action instanceof PDActionGoTo) {
			destination = ((PDActionGoTo) action).getDestination();
		}

		PDDocumentNameDictionary names = getPDDoc().getDocumentCatalog()
				.getNames();

		if (names != null) {

			PDDestinationNameTreeNode dests = names.getDests();

			if (destination instanceof PDNamedDestination) {
				String name = ((PDNamedDestination) destination)
						.getNamedDestination();
				destination = (PDDestination) dests.getValue(name);
			}

		}

		if (destination instanceof PDPageDestination) {

			itemObj.put("pageNumber", Integer
					.toString(((PDPageDestination) destination)
							.findPageNumber()));

		} else {

			itemObj.put("pageNumber", 1);

		}

		PDOutlineItem child = item.getFirstChild();

		itemObj.put("children", new JSONArray());

		while (child != null) {

			JSONObject childObj = itemToJson(child);

			((JSONArray) itemObj.get("children")).add(childObj);

			child = child.getNextSibling();

		}

		return itemObj;

	}

	/**
	 * Grava as informacões de outline no arquivo PDF.
	 * 
	 * @param Um
	 *            {@link JSONArray} com o outline
	 * @return Verdadeiro se não houver erros e a operaćão for bem sucedida.
	 * @throws Exception
	 *             Se algo der errado na gravaćão
	 */
	public Boolean save(JSONArray outlines, JSONArray annotations,
			OutputStream output) throws COSVisitorException, JDOMException,
			IOException {

		PDDocumentOutline outline = new PDDocumentOutline();

		getPDDoc().getDocumentCatalog().setDocumentOutline(outline);

		if (outlines != null) {

			for (Object jsonObj : outlines) {
				outline.appendChild(this.jsonToItem((JSONObject) jsonObj));
			}

		}

		if (annotations != null) {

			for (Object annotation : annotations) {

				JSONObject jobj = (JSONObject) annotation;

				JSONObject coords = jobj.getJSONObject("coords");

				String pageIndex = jobj.getString("pageId")
						.replace("pagediv", "").trim();

				addAnnotations(Integer.parseInt(pageIndex),
						coords.getDouble("w"), coords.getDouble("h"),
						coords.getDouble("l"), coords.getDouble("t"));

			}

		}

		getPDDoc().save(output);

		return true;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addAnnotations(int pgIndex, double cw, double ch, double cl,
			double ct) throws IOException {

		PDPage page = (PDPage) getPDDoc().getDocumentCatalog().getAllPages()
				.get(pgIndex);

		List annotations = page.getAnnotations();

		PDGamma colourRed = new PDGamma();
		colourRed.setR(1);

		PDBorderStyleDictionary borderThin = new PDBorderStyleDictionary();
		borderThin.setWidth(1);

		float pw = page.getMediaBox().getUpperRightX();
		float ph = page.getMediaBox().getUpperRightY();

		// double cw = obj.getJSONObject("cs").getDouble("w");
		// double ch = obj.getJSONObject("cs").getDouble("h");
		// double cl = obj.getJSONObject("cs").getDouble("l");
		// double ct = obj.getJSONObject("cs").getDouble("t");

		double mw = pw * cw;
		double mh = ph * ch;

		double ml = pw * cl;
		double mt = ph - ph * ct;

		PDRectangle position = new PDRectangle();

		PDAnnotationSquareCircle aCircle = new PDAnnotationSquareCircle(
				PDAnnotationSquareCircle.SUB_TYPE_SQUARE);
		aCircle.setColour(colourRed);
		aCircle.setBorderStyle(borderThin);

		position = new PDRectangle();

		position.setLowerLeftY(new Float(mt - mh));
		position.setLowerLeftX(new Float(ml));

		position.setUpperRightY(new Float(mt));
		position.setUpperRightX(new Float(ml + mw));

		aCircle.setRectangle(position);

		annotations.add(aCircle);

	}

	/**
	 * Fecha o {@link PDDocument}. Método obrigatório. Utilizá-lo sempre.
	 * Recomenda-se seu uso em cláusulas finnaly.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {

		if (getPDDoc() != null) {
			getPDDoc().close();
		}

	}

}
