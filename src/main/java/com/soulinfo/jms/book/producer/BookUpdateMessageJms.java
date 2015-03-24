package com.soulinfo.jms.book.producer;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.soulinfo.jms.AbstractMessageJms;

public class BookUpdateMessageJms extends AbstractMessageJms {
	
	@JacksonXmlProperty(localName="type")
	private String type;

	@JacksonXmlProperty(localName="event")
	private String event;
	
	@JacksonXmlProperty(localName="bookId")
	private String bookId;
	
	@JacksonXmlProperty(localName="pageId")
	private String pageId;
	
	@JacksonXmlProperty(localName="sectionId")
	private String sectionId;
	
	@JacksonXmlProperty(localName="printBookId")
	private String printBookId;
	
	@JacksonXmlProperty(localName = "pageList")
	private List<String> pageList;
	
	public List<String> getPageList() {
		return pageList;
	}

	public void setPageList(List<String> pageList) {
		this.pageList = pageList;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getBookId() {
		return bookId;
	}

	public void setBookId(String bookId) {
		this.bookId = bookId;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getSectionId() {
		return sectionId;
	}

	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	public String getPrintBookId() {
		return printBookId;
	}

	public void setPrintBookId(String printBookId) {
		this.printBookId = printBookId;
	}

}
