package com.supai.app.dao.dto;

import java.util.*;
import lombok.Data;

@Data
public class VersionRequest implements Cloneable, Request {
	private String otcsDocId;
	private String userId;
	private String gdriveDocId;
	private String otcsTicket;
	private String name;
	private String baseUrl;
	private boolean addingVersion;
	private String categoryId;
    private String feildIdOf_GDriveDocId;
    private String feildIdOf_EditorId;

		
	@Override
	public DocumentRequest clone() {
		try {
			return (DocumentRequest) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(); // can't happen since we implement Cloneable
		}
	}
	

}
