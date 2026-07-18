//package com.supai.app.services;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.ObjectFactory;
//import org.springframework.core.env.Environment;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.supai.app.config.GdriveYML;
//import com.supai.app.config.Otcs;
//import com.supai.app.config.OtcsCategory;
//import com.supai.app.dao.dto.DocMetadata;
//import com.supai.app.dao.dto.DocumentRequest;
//import com.supai.app.dao.dto.VersionRequest;
//import com.supai.app.services.common.JsonObj;
//import com.supai.app.services.common.MyUtil;
//import com.supai.app.services.gdrive.DeleteDocGDrive;
//import com.supai.app.services.gdrive.GDriveDownloader;
//import com.supai.app.services.gdrive.PermissionApiGdrive;
//import com.supai.app.services.gdrive.UploadFileContentGDrive;
//import com.supai.app.services.otcs.CatogeryApi;
//import com.supai.app.services.otcs.DownloadDoc;
//import com.supai.app.services.otcs.MembersApi;
//import com.supai.app.services.otcs.NodeApi;
//import com.supai.app.services.otcs.OtcsUploader;
//import com.supai.app.services.otcs.ReserveToggle;
//import com.supai.app.services.otcs.SearchDocs;
//import com.supai.app.services.otcs.api.auth.CurrentUserInfo;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class UsvService_ {
//	private static final Logger log = LoggerFactory.getLogger(UsvService_.class);
//
//	private final CurrentUserInfo currentUserInfo;
//	private final ObjectFactory<DocMetadata> metadataFactory;
//	private final CatogeryApi catogeryApi;
//	private final PermissionApiGdrive permissionApiGdrive;
//	private final DeleteDocGDrive deleteDocGDrive;
//	private final JsonObj jsonObj;
//	private final ObjectMapper objectMapper;
//	private final ReserveToggle reserveToggle;
//	private final GdriveYML gdriveYML;
//	private final GDriveDownloader gdriveDownloader;
//	private final OtcsUploader otcsUploader;
//	private final SearchDocs searchDocs;
//	private final DownloadDoc downloadDoc;
//	private final Environment environment;
//	private final OtcsCategory otcsCategory;
//	private final Otcs otcs;
//	private final CatogeryApi categoryApi;
//	private final NodeApi nodeApi;
//	private final MembersApi membersApi;
//	private final UploadFileContentGDrive uploadFileContentGDrive;
//	private final MyUtil myUtil;
//
//	public ResponseEntity<JsonNode> DocExportOtcsToGDrive(DocumentRequest documentRequest) {
//		try {
//			return exportOtcsToGDrive(documentRequest);
//		} catch (Exception e) {
//			JsonNode body = jsonObj.getJson("error", e.getMessage());
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
//		}
//	}
//	
//	public ResponseEntity<JsonNode> exportOtcsToGDrive(DocumentRequest documentRequest) {
//		DocMetadata docMetadata = metadataFactory.getObject();
//		docMetadata.setVal(documentRequest);
//		if (!checkCurrentUserAndApiCaller(docMetadata)) {
//			JsonNode jsonNode = jsonObj.getJson("{\"error\": \"Api Caller and userID did not matched.\"}");
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
//					.body(jsonNode);
//		}
//
//		JsonNode propertyDetails = nodeApi.getNodesProperty(docMetadata);
//		log.info("{}", propertyDetails.toString());
//
//		boolean isResurved = propertyDetails.at("/data/properties/reserved").asBoolean();
//		String name = propertyDetails.at("/data/properties/name").asText();
//		String userId = propertyDetails.at("/data/properties/reserved_user_id").asText();
////		
////		log.info(request.toString());
////		log.info(isResurved + " : " + userId);
//		docMetadata.setName(name);
//
//		if (isResurved == true) {
//			if (userId.equals(documentRequest.getUserId())) {
//				String feildId = (String) otcsCategory.getFeildNames().get("GDriveDocId");
//				JsonNode gdriveDocId = catogeryApi.getCatogeryDetailsById(docMetadata)
//						.at("/data/categories/{feildId}".replace("{feildId}", feildId));
//				return ResponseEntity
//						.ok(objectMapper.valueToTree(objectMapper.createObjectNode().set("id", gdriveDocId)));
//			} else {
//				JsonNode jsonNode = jsonObj
//						.getJson("{\"error\": \"The item '{name}' is reserved.\"}".replace("{name}", name));
//				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
//						.body(jsonNode);
//			}
//		} else {
//			reserveToggle.reserveDoc(documentRequest);
//		}
//
//		ResponseEntity<byte[]> docResponse = downloadDoc.GetDoc(documentRequest, name);
//
//		ResponseEntity<JsonNode> uploadResponse = uploadFileContentGDrive.uploadToGdrive(docResponse, name,
//				gdriveYML.getGdriveRoot(), documentRequest);
//		permissionApiGdrive.shareFileWithUser(uploadResponse.getBody().path("id").asText(), documentRequest.getEmail());
//
//		Map<String, String> dataMap = new HashMap<>();
//		dataMap.put("baseUrl", documentRequest.getBaseUrl());
//		dataMap.put("otcsDocId", documentRequest.getOtcsDocId());
//		dataMap.put("otcsTicket", documentRequest.getOtcsTicket());
//		categoryApi.deleteCategory(dataMap);
//		categoryApi.applyCategory(documentRequest, uploadResponse.getBody().path("id").asText());
//		return uploadResponse;
//	}
//
//	private boolean checkCurrentUserAndApiCaller(DocMetadata docMetadata) {
//		// TODO Auto-generated method stub
//		ResponseEntity<String> userInfoResponse = currentUserInfo.getCurrentUserInfo(docMetadata);
//		return userInfoResponse.getBody().contains(docMetadata.getUserId());
//	}
//
//	public ResponseEntity<String> addVersionToDoc(VersionRequest versionRequest) {
//		DocMetadata docMetadata = metadataFactory.getObject();
//		docMetadata.setVal(versionRequest);
//
//		if (!checkCurrentUserAndApiCaller(docMetadata)) {
//			Map<String, String> error = new HashMap<>();
//			error.put("error", "Api Caller and userID did not matched.");
//
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
//					.body(jsonObj.mapToJsonString(error));
//		}
//
////		JsonNode propertyDetails = nodeApi.getNodesProperty(docMetadata);
//
//		reserveToggle.unreserveDoc(docMetadata);
//		ResponseEntity<byte[]> gdriveDocResponse = null;
//		ResponseEntity<String> otcsResponse = null;
//		if (versionRequest.isAddingVersion()) {
//			gdriveDocResponse = gdriveDownloader.getDriveDoc(versionRequest.getGdriveDocId());
//			otcsResponse = otcsUploader.addVersionToDoc(versionRequest, gdriveDocResponse);
//		} else {
//			otcsResponse = ResponseEntity.ok().body("{\"msg\":\"Changes discarded.\"}");
//		}
//
//		Map<String, String> dataMap = new HashMap<>();
//		dataMap.put("baseUrl", versionRequest.getBaseUrl());
//		dataMap.put("otcsDocId", versionRequest.getOtcsDocId());
//		dataMap.put("otcsTicket", versionRequest.getOtcsTicket());
//		categoryApi.deleteCategory(dataMap);
////		categoryApi.updateCategory(dataMap);
//		deleteDocGDrive.removeDriveDoc(versionRequest.getGdriveDocId());
//		return otcsResponse;
//	}
//}