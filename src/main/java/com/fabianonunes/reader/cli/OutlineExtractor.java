package com.fabianonunes.reader.cli;

import java.io.IOException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

public class OutlineExtractor {

	private PDDocument pddoc;

	public JSONArray getAllBookmarks() throws IOException {

		JSONArray outline = getOutlineAsJSON();
		JSONObject root = new JSONObject();
		root.put("children", outline);

		JSONArray plain = new JSONArray();
		plain.addAll(toPlainArray(root, false));

		return plain;

	}

	private static JSONArray toPlainArray(JSONObject jobj, boolean addMe) {

		JSONArray r = new JSONArray();

		if (jobj.has("children")) {

			JSONArray children = jobj.getJSONArray("children");

			for (Object child : children) {

				r.addAll(toPlainArray((JSONObject) child, true));

			}

			jobj.remove("children");

		}
		if (addMe) {
			r.add(jobj);
		}

		return r;
	}

	public OutlineExtractor() {
	}

	public OutlineExtractor(PDDocument pddoc) {
		this.pddoc = pddoc;
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

		itemObj.put("text", item.getTitle());
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
