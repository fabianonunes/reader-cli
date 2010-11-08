package com.fabianonunes.reader.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class PageExtractor {

	PDDocument pddoc;

	PDDocument newDoc;

	public PageExtractor(PDDocument pddoc) {

		this.pddoc = pddoc;

	}

	public List<PDPage> filterPages(String pattern) throws IOException {

		OutlineExtractor out = new OutlineExtractor(pddoc);

		JSONArray outline = out.getAllBookmarks();

		List<PDPage> pages = new ArrayList<PDPage>();

		for (Object o : outline) {

			JSONObject jobj = (JSONObject) o;

			if (jobj.get("pageNumber") == null) {
				continue;
			}

			int pageNumber = jobj.getInt("pageNumber");

			String text = jobj.getString("text").trim();

			if (pageNumber > 0 && text.matches(pattern)) {

				pages.add(getPage(pageNumber - 1));

			}

		}

		return pages;

	}

	@SuppressWarnings("unchecked")
	public PDPage getPage(int pageIndex) {

		List<PDPage> pages = pddoc.getDocumentCatalog().getAllPages();

		if (pageIndex >= pages.size()) {

			return null;

		}

		Object page = pages.get(pageIndex);

		if (page instanceof PDPage) {

			return (PDPage) page;

		} else {

			return null;

		}

	}
}
