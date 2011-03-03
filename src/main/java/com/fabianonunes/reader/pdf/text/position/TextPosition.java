package com.fabianonunes.reader.pdf.text.position;

public interface TextPosition {

	public String calculatePositions(Integer pageNumber) throws Throwable;

	String getXmlDoc(Integer pageNumber) throws Throwable;

}
