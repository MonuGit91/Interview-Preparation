package com.supai.app.dao.dto;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Component
@Scope("prototype")
public class DocMetadata {
	private String name;
	private String otcsDocId;
	private String userId;
	private String email;
	private String otcsTicket;
	private String baseUrl;
	private String gdriveDocId;
	private String categoryId;
	private String feildIdOf_GDriveDocId;
	private String feildIdOf_EditorId;
	
//	public DocMetadata(){}
	
//	public DocMetadata(Object request){
//		if(request instanceof DocumentRequest) {
//			DocumentRequest req = (DocumentRequest) request;
//			this.name = req.getBaseUrl();
//			this.email = req.getEmail();
//			this.otcsDocId = req.getOtcsDocId();
//			this.userId = req.getUserId();
//			this.baseUrl = req.getBaseUrl();
//			this.otcsTicket = req.getOtcsTicket();
//		} else if(request instanceof VersionRequest) {
//			VersionRequest req = (VersionRequest) request;
//			this.name = req.getName();
//			this.otcsDocId = req.getOtcsDocId();
//			this.otcsTicket = req.getOtcsTicket();
//			this.gdriveDocId = req.getGdriveDocId();
//			this.baseUrl = req.getBaseUrl();
//		} else {
//			log.error("Unexpected type provide!");
//		}
//	}

	public void setVal(Object request) {
		if(request instanceof DocumentRequest) {
			DocumentRequest req = (DocumentRequest) request;
			this.name = req.getBaseUrl();
			this.email = req.getEmail();
			this.otcsDocId = req.getOtcsDocId();
			this.userId = req.getUserId();
			this.baseUrl = req.getBaseUrl();
			this.otcsTicket = req.getOtcsTicket();
			this.categoryId = req.getCategoryId();
			this.feildIdOf_EditorId = req.getFeildIdOf_EditorId();
			this.feildIdOf_GDriveDocId = req.getFeildIdOf_GDriveDocId();
		} else if(request instanceof VersionRequest) {
			VersionRequest req = (VersionRequest) request;
			this.name = req.getName();
			this.otcsDocId = req.getOtcsDocId();
			this.userId = req.getUserId();
			this.otcsTicket = req.getOtcsTicket();
			this.gdriveDocId = req.getGdriveDocId();
			this.baseUrl = req.getBaseUrl();
			this.categoryId = req.getCategoryId();
			this.feildIdOf_EditorId = req.getFeildIdOf_EditorId();
			this.feildIdOf_GDriveDocId = req.getFeildIdOf_GDriveDocId();
		} else {
			log.error("Unexpected type provide!");
		}
		
	}
}
