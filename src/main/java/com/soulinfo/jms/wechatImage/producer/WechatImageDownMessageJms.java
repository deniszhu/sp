package com.soulinfo.jms.wechatImage.producer;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.soulinfo.jms.AbstractMessageJms;

public class WechatImageDownMessageJms extends AbstractMessageJms {
	
	@JacksonXmlProperty(localName="albumId")
	private String albumId;

	@JacksonXmlProperty(localName="imageOwnerId")
	private String imageOwnerId;
	
	@JacksonXmlProperty(localName="media_id")
	private String media_id;
	
	@JacksonXmlProperty(localName="access_token")
	private String access_token;

	public String getAlbumId() {
		return albumId;
	}

	public void setAlbumId(String albumId) {
		this.albumId = albumId;
	}

	public String getImageOwnerId() {
		return imageOwnerId;
	}

	public void setImageOwnerId(String imageOwnerId) {
		this.imageOwnerId = imageOwnerId;
	}

	public String getMedia_id() {
		return media_id;
	}

	public void setMedia_id(String media_id) {
		this.media_id = media_id;
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}
	

	


}
