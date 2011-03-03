package com.fabianonunes.reader.pdf.outline;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class OutlineHandler {

	JSONArray bookmarks = new JSONArray();

	public OutlineHandler(Map<String, List<Integer>> map) {

		if (map == null || map.size() == 0) {
			return;
		}

		for (String name : map.keySet()) {

			List<Integer> pages = map.get(name);

			for (Integer pageNumber : pages) {

				addItem(name, pageNumber, null);

			}

		}

	}

	public OutlineHandler() {
	}

	public JSONObject addItem(String text, Integer pageNumber,
			JSONArray children) {

		JSONObject item = new JSONObject();

		item.put("text", text);

		item.put("pageNumber", pageNumber);

		if (children != null) {
			item.put("children", children);
		} else {
			item.put("children", new JSONArray());
		}

		bookmarks.add(item);

		return item;

	}

	public void addItemTo(JSONObject stack, JSONObject item) {

		if (stack.has("children")) {

			Object children = stack.get("children");

			if (!(children instanceof JSONArray)) {
				children = new JSONArray();
			}

			((JSONArray) children).add(item);

		}

	}

	public JSONArray getRoot() {
		return bookmarks;
	}

}
