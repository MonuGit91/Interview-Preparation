package com.supai.app.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.GdriveYML;
import com.supai.app.dao.dto.DocMetadata;
import com.supai.app.dao.dto.DocumentRequest;
import com.supai.app.dao.dto.VersionRequest;
import com.supai.app.exception.PermissionException;
import com.supai.app.services.common.JsonObj;
import com.supai.app.services.gdrive.DeleteDocGDrive;
import com.supai.app.services.gdrive.GDriveDownloader;
import com.supai.app.services.gdrive.PermissionApiGdrive;
import com.supai.app.services.gdrive.UploadFileContentGDrive;
import com.supai.app.services.otcs.CatogeryApi;
import com.supai.app.services.otcs.DownloadDoc;
import com.supai.app.services.otcs.NodeApi;
import com.supai.app.services.otcs.OtcsUploader;
import com.supai.app.services.otcs.ReserveToggle;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsvService {
	private static final Logger log = LoggerFactory.getLogger(UsvService.class);

	private final ObjectFactory<DocMetadata> metadataFactory;
	private final CatogeryApi catogeryApi;
	private final PermissionApiGdrive permissionApiGdrive;
	private final DeleteDocGDrive deleteDocGDrive;
	private final JsonObj jsonObj;
	private final ObjectMapper objectMapper;
	private final ReserveToggle reserveToggle;
	private final GdriveYML gdriveYML;
	private final GDriveDownloader gdriveDownloader;
	private final OtcsUploader otcsUploader;
	private final DownloadDoc downloadDoc;
	private final CatogeryApi categoryApi;
	private final NodeApi nodeApi;
	private final UploadFileContentGDrive uploadFileContentGDrive;

	public ResponseEntity<JsonNode> DocExportOtcsToGDrive(DocumentRequest documentRequest) {
		try {
			return exportOtcsToGDrive(documentRequest);
		} catch (Exception e) {
			JsonNode body = jsonObj.getJson(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
		}
	}
	
	public ResponseEntity<JsonNode> exportOtcsToGDrive(DocumentRequest documentRequest) throws Exception {
		DocMetadata docMetadata = metadataFactory.getObject();
		docMetadata.setVal(documentRequest);
//		checkCurrentUserAndApiCaller(docMetadata);

		JsonNode propertyDetails = nodeApi.getNodesProperty(docMetadata);
		log.info("{}", propertyDetails.toString());

		boolean isResurved = propertyDetails.at("/data/properties/reserved").asBoolean();
		String name = propertyDetails.at("/data/properties/name").asText();
		String userId = propertyDetails.at("/data/properties/reserved_user_id").asText();

		docMetadata.setName(name);

		if (isResurved == true) {
			String otcsFeildId = documentRequest.getFeildIdOf_GDriveDocId();
			JsonNode gdriveDocId = catogeryApi.getCatogeryDetailsById(docMetadata)
					.at("/data/categories/{feildId}".replace("{feildId}", otcsFeildId));
			if (!userId.equals(documentRequest.getUserId())) {
				permissionApiGdrive.grantPermission(gdriveDocId.asText(), documentRequest.getEmail(), false);
			}
			
			return ResponseEntity
					.ok(objectMapper.valueToTree(objectMapper.createObjectNode().set("id", gdriveDocId)));
		} else {
			reserveToggle.reserveDoc(documentRequest);
		}

		ResponseEntity<byte[]> docResponse = downloadDoc.GetDoc(documentRequest, name);

		ResponseEntity<JsonNode> uploadResponse = uploadFileContentGDrive.uploadToGdrive(docResponse, name,
				gdriveYML.getGdriveRoot(), documentRequest);
		permissionApiGdrive.grantPermission(uploadResponse.getBody().path("id").asText(), documentRequest.getEmail(), true);

		Map<String, String> dataMap = new HashMap<>();
		dataMap.put("baseUrl", documentRequest.getBaseUrl());
		dataMap.put("otcsDocId", documentRequest.getOtcsDocId());
		dataMap.put("otcsTicket", documentRequest.getOtcsTicket());
		dataMap.put("categoryId", documentRequest.getCategoryId());
		dataMap.put("full_feildIdOf_EditorId", documentRequest.getFeildIdOf_EditorId());
		dataMap.put("feildIdOf_GDriveDocId", documentRequest.getFeildIdOf_GDriveDocId());
		categoryApi.deleteCategory(dataMap);
		categoryApi.applyCategory(documentRequest, uploadResponse.getBody().path("id").asText());
		return uploadResponse;
	}

//	private void checkCurrentUserAndApiCaller(DocMetadata docMetadata) throws Exception {
//		// TODO Auto-generated method stub
//		ResponseEntity<JsonNode> userInfoResponse = currentUserInfo.getCurrentUserInfo(docMetadata);
//		if(!userInfoResponse.getBody().contains(docMetadata.getUserId())) {
//			String error = jsonObj.getJson("error", "Api Caller and userID did not matched.").toString();
//			log.error(error);
//			throw new IllegalAccessException(error);
//		}
//	}

	public ResponseEntity<String> addVersionToDocument(VersionRequest versionRequest) throws Exception  {	
		
		DocMetadata docMetadata = metadataFactory.getObject();
		docMetadata.setVal(versionRequest);
		
		String otcsUserFeildId = versionRequest.getFeildIdOf_EditorId();
		JsonNode otcsUserFeildValue = catogeryApi.getCatogeryDetailsById(docMetadata)
				.at("/data/categories/{feildId}".replace("{feildId}", otcsUserFeildId));
		
		if(!versionRequest.getUserId().trim().equals(otcsUserFeildValue.asText().trim())) {
			String msg = new PermissionException().getMessage();
			log.info(msg);
			return ResponseEntity.badRequest().body(msg);
		}

		reserveToggle.unreserveDoc(docMetadata);
		ResponseEntity<byte[]> gdriveDocResponse = null;
		ResponseEntity<String> otcsResponse = null;
		if (versionRequest.isAddingVersion()) {
			gdriveDocResponse = gdriveDownloader.getDriveDoc(versionRequest.getGdriveDocId());
			otcsResponse = otcsUploader.addVersionToDoc(versionRequest, gdriveDocResponse);
		} else {
			otcsResponse = ResponseEntity.ok().body(jsonObj.getJson("message", "Changes discarded.").toString());
		}

		Map<String, String> dataMap = new HashMap<>();
		dataMap.put("baseUrl", versionRequest.getBaseUrl());
		dataMap.put("otcsDocId", versionRequest.getOtcsDocId());
		dataMap.put("otcsTicket", versionRequest.getOtcsTicket());
		dataMap.put("categoryId", versionRequest.getCategoryId());
		dataMap.put("full_feildIdOf_EditorId", versionRequest.getFeildIdOf_EditorId());
		dataMap.put("feildIdOf_GDriveDocId", versionRequest.getFeildIdOf_GDriveDocId());
		categoryApi.deleteCategory(dataMap);
//		categoryApi.updateCategory(dataMap);
		deleteDocGDrive.removeDriveDoc(versionRequest.getGdriveDocId());
		return otcsResponse;
	
	}
	public ResponseEntity<String> addVersionToDoc(VersionRequest versionRequest){
		try {
			return addVersionToDocument(versionRequest);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}
}

