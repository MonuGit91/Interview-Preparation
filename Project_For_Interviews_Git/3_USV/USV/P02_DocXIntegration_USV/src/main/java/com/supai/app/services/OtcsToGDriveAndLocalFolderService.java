//package com.supai.app.services;
//
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.env.Environment;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.fasterxml.jackson.databind.node.TextNode;
//import com.supai.app.config.Otcs;
//import com.supai.app.config.OtcsCategory;
//import com.supai.app.services.gdrive.CreateFolderGDrive;
//import com.supai.app.services.gdrive.IsDocPresentGDrive;
//import com.supai.app.services.gdrive.ListSubFilderGdrive;
//import com.supai.app.services.gdrive.UploadFileContentGDrive;
//import com.supai.app.services.otcs.CatogeryApi;
//import com.supai.app.services.otcs.DownloadDoc;
//import com.supai.app.services.otcs.MembersApi;
//import com.supai.app.services.otcs.NodeApi;
//import com.supai.app.services.otcs.SearchDocs;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class OtcsToGDriveAndLocalFolderService {
//	private static final Logger log = LoggerFactory.getLogger(OtcsToGDriveAndLocalFolderService.class);
//	private final SearchDocs searchDocs;
//	private final DownloadDoc downloadDoc;
//	private final Environment environment;
//	private final OtcsCategory otcsCategory;
//	private final Otcs otcs;
//	private final CatogeryApi categoryApi;
//	private final NodeApi nodeApi;
//	private final MembersApi membersApi;
//	private int docNumber = 1;
//	private String otcsRootFolderName;
//	private final CreateFolderGDrive createFolderGDrive;
//	private final IsDocPresentGDrive isDocPresentGDrive;
//	private final ListSubFilderGdrive listSubFilderGdrive;
//	private final UploadFileContentGDrive uploadFileContentGDrive;
//	private static StringBuilder csvContent = new StringBuilder();
//	private static StringBuilder csvHeading = new StringBuilder();
//
//	public void exportOtcsToGDriveAndLocal() {
//		String otcsRootFolderId = environment.getProperty("otcs.root-folder-id");
//		otcsRootFolderName = nodeApi.getNodesProperty(otcsRootFolderId).path("data").path("properties").path("name")
//				.asText();
//		folderIterator(otcsRootFolderId, otcsRootFolderName + "/", "0");
//
//		// ------------------Upload Meta data to Gdrive start-----------------
//		ResponseEntity<byte[]> metadataResponseEntity = buildMetadateResponseEntity();
//		createFolderAndUploadDocToGDrive(metadataResponseEntity, otcsRootFolderName.split("/"),
//				"123_id_can_be_any_thing", "metadata.csv");
//		// ------------------Upload Meta data to Gdrive end-----------------
//	}
//
//	private void folderIterator(String parentId, String parentFolderPath, String type) {
//		JsonNode jsonNode = null;
//		try {
//			jsonNode = searchDocs.getChildNodes(parentId);
//			// Iterate over the array
//			for (JsonNode node : jsonNode) {
//				JsonNode properties = node.path("data").path("properties");
//				type = properties.path("type").toString();
//				String id = properties.path("id").asText();
//				String name = properties.path("name").asText();
//
//				if (otcs.getParentTypes().containsKey(type)) {
//					folderIterator(id, parentFolderPath + name + "/", type);
//				} else if (type.equals("144")) {
//					log.info("---------------------Document {} start---------------------", docNumber);
//					log.info("name: {}", name);
//					log.info("id: {}", id);
//
//					JsonNode categoryDetails = null, propertyDetails = null, userDetails = null, metadata = null;
//					ResponseEntity<byte[]> docResponse = null;
//
//					categoryDetails = categoryApi.getCatogeryDetailsById(id);
//					if (categoryDetails != null) {
//						propertyDetails = nodeApi.getNodesProperty(id);
//						userDetails = membersApi.getUserInfo(
//								propertyDetails.path("data").path("properties").path("create_user_id").asText());
//						metadata = filterMetadata(categoryDetails, propertyDetails, userDetails, parentFolderPath, name,
//								null, null);
//						docResponse = downloadDoc.GetDoc(id, name);
//					}
//					if (docResponse == null || metadata == null || categoryDetails == null) {
//						if (categoryDetails == null)
//							log.info("Skipping dodument because Category Details is not found");
//						continue;
//					}
//
//					// ----------------------Save Document to Local start-----------------------
//					boolean isSuccess = true;
//					if (createFolder(parentFolderPath)) {
//						saveFile(parentFolderPath, name, docResponse.getBody());
//						exportMetadataToRelativeFolder(metadata, otcsRootFolderName, "metadata.csv");
//						// exportDifferentMetadataToDifferentDoc(metadata, parentFolderPath, name);
//					}
//					if (isSuccess)
//						log.info("document export to Local successful");
//					else
//						log.info("document export to Local failed");
//					// ----------------------Save Document to Local end-----------------------
//
//					// -----------------Upload to GDrive Start-------------------------
//					createMetadataContent(metadata, otcsRootFolderName, "metadata.csv");
//					createFolderAndUploadDocToGDrive(docResponse, parentFolderPath.split("/"), id, name);
//					// -----------------Upload to GDrive End-------------------------
//					log.info("---------------------Document {} end---------------------", docNumber++);
//				}
//			}
//		} catch (Exception e) {
//			log.info("Exception while iterating over otcs folder - {}", e.getMessage());
//		}
//	}
//
//	private JsonNode filterMetadata(JsonNode categoryDetails, JsonNode propertyDetails, JsonNode userDetails,
//			String parentIdPath, String name, Map<String, Object> categoryFields, ObjectNode finalJson) {
//		// TODO Auto-generated method stub
//		try {
//			final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
//			JsonNode categoriesNode = categoryDetails.path("data").path("categories");
//			JsonNode propertiesNode = propertyDetails.path("data").path("properties");
//			JsonNode userNode = userDetails.path("data").path("properties");
//			Map<String, String> userFeildNames = otcsCategory.getUserFeildNames();
//			ObjectMapper objectMapper = new ObjectMapper();
//
//			finalJson = finalJson != null ? finalJson : objectMapper.createObjectNode();
//			categoryFields = categoryFields != null ? categoryFields : otcsCategory.getFeildNames();
//			for (Map.Entry<String, Object> entry : categoryFields.entrySet()) {
//				String categoryFieldName = entry.getKey();
//				Object entryValue = entry.getValue();
//
//				if (categoryFieldName.equals("Name")) {
//					finalJson.set(categoryFieldName, new TextNode(name));
//				} else if (categoryFieldName.equals("Path")) {
//					finalJson.set(categoryFieldName, objectMapper
//							.valueToTree(Paths.get(BASE_DIRECTORY, parentIdPath, name).toString().replace("\\", "/")));
//				} else if (categoryFieldName.equals("CreatedBy")) {
//					finalJson.set("Created By", userNode.path("name"));
//				} else if (categoryFieldName.equals("CreatedOn")) {
//					finalJson.set("Created On", propertiesNode.path("create_date"));
//				} else if (entryValue instanceof Map) {
//					filterMetadata(categoryDetails, propertyDetails, userDetails, parentIdPath, name,
//							(Map<String, Object>) entryValue, finalJson);
//				} else if (entryValue instanceof String) {
//					String categoryFieldKey = (String) entryValue;
//					boolean isUser = false;
//					if (userFeildNames.containsKey(categoryFieldName)) {
//						String userId = categoriesNode.path(categoryFieldKey).asText();
//						JsonNode userName = membersApi.getUserInfo(userId).path("data").path("properties").path("name");
//						finalJson.set(categoryFieldName, userName);
//						isUser = true;
//					}
//					if (!isUser) {
//						if (!categoryFieldKey.contains("x")) {
//							finalJson.set(categoryFieldName, categoriesNode.path(categoryFieldKey));
//						} else {
//							List<String> categoryValues = new ArrayList<>();
//							String feildId = categoryFieldKey.replace("x", "1");
//							for (int rowNum = 1; !categoriesNode.path(feildId).isMissingNode(); rowNum++) {
//								feildId = categoryFieldKey.replace("x", "" + rowNum);
//								String feildValue = categoriesNode.path(feildId).asText().trim();
//								categoryValues.add(feildValue);
//							}
//							categoryValues.remove(categoryValues.size() - 1); // because at the end one empty feild is
//																				// added
//							String result = categoryValues.toString().replace(",", " | ");
//							finalJson.set(categoryFieldName, TextNode.valueOf(result));
//						}
//
//					}
//				}
//
//			}
//
//			return finalJson;
//		} catch (Exception e) {
//			log.info("Error: Exception while creating finla metadata ", e.getMessage());
//		}
//
//		return null;
//	}
//
//	private ResponseEntity<byte[]> buildMetadateResponseEntity() {
//		String finalCsv = csvHeading.toString() + "\n" + csvContent.toString();
//		byte[] csvBytes = finalCsv.getBytes(StandardCharsets.UTF_8);
//
//		// Step 3: Prepare fake ResponseEntity<byte[]> (mimicking downloaded file)
//		ResponseEntity<byte[]> csvEntity = new ResponseEntity<>(csvBytes, HttpStatus.OK);
//		return csvEntity;
//	}
//
//	private void createMetadataContent(JsonNode metadata, String filename, String folderId) {
//		StringBuilder currentCsvHeading = new StringBuilder();
//		StringBuilder currentCsvContent = new StringBuilder();
//
//		Iterator<Map.Entry<String, JsonNode>> fields = metadata.fields();
//		while (fields.hasNext()) {
//			Map.Entry<String, JsonNode> entry = fields.next();
//			currentCsvHeading.append(entry.getKey()).append(",");
//			String entryValue = entry.getValue().toString().replace(",", " | ").replace("null", "");
//			currentCsvContent.append(entryValue).append(",");
//		}
//		if (csvHeading.isEmpty())
//			csvHeading = currentCsvHeading;
//		csvContent.append(currentCsvContent).append("\n");
//
//	}
//
//	public void exportMetadataToRelativeFolder(JsonNode metadata, String relativeExpoertPaht, String filename) {
//		try {
//			final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
//			Path csvFilePath = Paths.get(BASE_DIRECTORY, otcsRootFolderName, "metadata.csv");
//			boolean fileExists = Files.exists(csvFilePath);
//
//			StringBuilder csvContent = new StringBuilder();
//			StringBuilder csvHeadings = new StringBuilder();
//
//			Iterator<Map.Entry<String, JsonNode>> fields = metadata.fields();
//			while (fields.hasNext()) {
//				Map.Entry<String, JsonNode> entry = fields.next();
//				if (!fileExists)
//					csvHeadings.append(entry.getKey()).append(",");
//				String entryValue = entry.getValue().toString().replace(",", " | ").replace("null", "");
//				csvContent.append(entryValue).append(",");
//			}
//
//			if (!fileExists)
//				Files.write(csvFilePath, csvHeadings.append("\n").toString().getBytes(), StandardOpenOption.CREATE,
//						StandardOpenOption.APPEND);
//			Files.write(csvFilePath, csvContent.append("\n").toString().getBytes(), StandardOpenOption.APPEND);
//
//		} catch (Exception e) {
//			System.err.println("❌ Error writing CSV file: ");
//			e.printStackTrace();
//		}
//	}
//
//	private void saveFile(String folderPath, String fileName, byte[] fileData) {
//		final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
//		Path filePath = null;
//		try {
//			filePath = Paths.get(BASE_DIRECTORY, folderPath, fileName);
//			Files.write(filePath, fileData);
//		} catch (Exception e) {
//			System.err.println("❌ Error Saving File: " + filePath.toString());
//			e.printStackTrace();
//		}
//	}
//
//	private boolean createFolder(String folderPath) {
//		final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
//		try {
//			Path localPath = Paths.get(BASE_DIRECTORY, folderPath);
//			Files.createDirectories(localPath);
//		} catch (Exception e) {
//			System.err.println("❌ Error Creating Folder: " + folderPath);
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}
//
//	private void createFolderAndUploadDocToGDrive(ResponseEntity<byte[]> responseDoc, String[] parentFolderPathArr,
//			String docId, String docName) {
//		log.info("Uploading document to Gdrive...");
//		// Get root folder ID from environment and search for or create the spend
//		String rootFolderIdInGdrive = environment.getProperty("gdrive.folder.root-Supai");
//		String parentFolderId = "";
//		String parentFolder = parentFolderPathArr[parentFolderPathArr.length - 1];
//		for (int i = 0; i < parentFolderPathArr.length; i++) {
//			parentFolderId = getFolderIdFromGdrive(parentFolderPathArr[i],
//					parentFolderId.isEmpty() ? rootFolderIdInGdrive : parentFolderId);
//		}
//
//		if (parentFolder == null) {
//			log.info("Error: Can not upload to GDrive because Parent Folder is null!");
//			return;
//		}
//
//		// Check if the document is already present in Google Drive to avoid duplicates
//		String correctParentFolderId = parentFolderId;
//		ResponseEntity<String> responseIsDocExist = isDocPresentGDrive.isDocPresentOnGdrive(docName,
//				correctParentFolderId);
//		boolean isDocExist = ((responseIsDocExist == null) || (responseIsDocExist.getBody() == null)
//				|| responseIsDocExist.getBody().contains(docName));
//
//		if (isDocExist) {// This if block update the name of doc and upload to gdrive.
//			if (docName == "metadata.csv" && responseDoc != null) {
//				// This block will run only when doc name is matadata.csv is present on gdrive.
//				// updateName Method will update the doc name like
//				// oldName_<date>_<time>.<extention>
//				String newDocName = updateName(docName);
//				uploadFileContentGDrive.uploadToGdrive(responseDoc, parentFolder, newDocName, docId,
//						rootFolderIdInGdrive, correctParentFolderId);
//			} else {
//				log.info("Skipping uploading to GDrive because it is already present on Gdrive.");
//			}
//		} else if (responseDoc != null) {
//			// If the document does not exist in Google Drive, then upload it to Google
//			// Drive
//			uploadFileContentGDrive.uploadToGdrive(responseDoc, parentFolder, docName, docId, rootFolderIdInGdrive,
//					correctParentFolderId);
//		}
//
//	}
//
//	public void upload() {
//		// Attempt to search for all documents in OTCS
//		JsonNode resultsNode = searchDocs.getDocs();
//
//		if (resultsNode == null || !resultsNode.isArray()) {
//			log.info("Faild: Documents Searching Faild");
//			return;
//		}
//
//		log.info(resultsNode.size() + " documents found");
//
//		// Iterate over each result document in the search results array
//		int docNumber = 1;
//		for (JsonNode result : resultsNode) {
//			String docId = result.path("data").path("properties").path("id").asText();
//			String docName = result.path("data").path("properties").path("name").asText();
//			String spendCategory = result.path("data").path("regions").path(otcsCategory.getAttr()).asText();
//
//			// Check if any critical field is empty
//			if (docId.isEmpty() || docName.isEmpty() || spendCategory.isEmpty()) {
//				if (docId.isEmpty()) {
//					log.info("Failed: Document ID is missing or empty");
//				}
//				if (docName.isEmpty()) {
//					log.info("Failed: Document Name is missing or empty");
//				}
//				if (spendCategory.isEmpty()) {
//					log.info("Failed: Spend Category is missing or empty");
//				}
//				continue;
//			}
//
//			// Check that document ID, name, and spend category are non-empty
//			log.info("---------------------Document {} start---------------------", docNumber);
//			log.info("Uploading document having docId: {}, name: {}, spendCategory: {}", docId, docName, spendCategory);
//
//			// Get root folder ID from environment and search for or create the spend
//			String rootFolderIdInGdrive = environment.getProperty("gdrive.folder.root-Supai");
//			String spendCategoryIdInGdrive = getFolderIdFromGdrive(spendCategory, rootFolderIdInGdrive);
//
//			if (spendCategoryIdInGdrive == null) {
//				log.info("---------------------Document {} End ---------------------\n", docNumber++);
//				return;
//			}
//
//			// Check if the document is already present in Google Drive to avoid duplicates
//			ResponseEntity<String> responseIsDocExist = isDocPresentGDrive.isDocPresentOnGdrive(docName,
//					spendCategoryIdInGdrive);
//			boolean isDocExist = ((responseIsDocExist == null) || (responseIsDocExist.getBody() == null)
//					|| responseIsDocExist.getBody().contains(docName));
//
//			// If the document does not exist in Google Drive, download it from OTCS and
//			// upload it to Google Drive
//			if (isDocExist) {
//				String newDocName = updateName(docName);
//				ResponseEntity<byte[]> responseDoc = downloadDoc.GetDoc(docId, docName);
//				if (responseDoc != null) {
//					uploadFileContentGDrive.uploadToGdrive(responseDoc, spendCategory, newDocName, docId,
//							rootFolderIdInGdrive, spendCategoryIdInGdrive);
//				}
//			} else {
//				ResponseEntity<byte[]> responseDoc = downloadDoc.GetDoc(docId, docName);
//				if (responseDoc != null) {
//					uploadFileContentGDrive.uploadToGdrive(responseDoc, spendCategory, docName, docId,
//							rootFolderIdInGdrive, spendCategoryIdInGdrive);
//				}
//			}
//			log.info("---------------------Document {} End ---------------------\n", docNumber++);
//
//		}
//	}
//
//	private String updateName(String docName) {
//		// Date and time format: ddMMyyyy_HHmmss
//		String originalFileName = docName; // Example without extension
//
//		// Date and time format: ddMMyyyy_HHmmss
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");
//
//		// Get the current date and time from the server
//		LocalDateTime now = LocalDateTime.now();
//
//		// Format the current date and time
//		String formattedDateTime = now.format(formatter);
//
//		// Check if the file has an extension
//		String name;
//		String extension = "";
//		int lastDotIndex = originalFileName.lastIndexOf('.');
//
//		if (lastDotIndex != -1) {
//			// File has an extension
//			name = originalFileName.substring(0, lastDotIndex); // Name without extension
//			extension = originalFileName.substring(lastDotIndex); // Extension with dot
//		} else {
//			// File has no extension
//			name = originalFileName;
//		}
//
//		// Append the formatted date and time between name and extension
//		String finalFileName = name + "_" + formattedDateTime + extension;
//
//		return finalFileName;
//	}
//
//	private String getFolderIdFromGdrive(String folderToFind, String rootFolderId) {
//		if (folderToFind == null || rootFolderId == null) {
//			return null;
//		}
//
//		// Fetch the list of subfolders under the root folder to check for the specified
//		// folder
//		JsonNode subFolders = listSubFilderGdrive.fetchSubFolders(rootFolderId);
//		if (subFolders == null || subFolders.has("error")) {
//			return null;
//		}
//
//		JsonNode filesNode = subFolders.get("files");
//		if (filesNode.isArray()) {
//			for (JsonNode fileNode : filesNode) {
//				String id = fileNode.get("id").asText();
//				String name = fileNode.get("name").asText();
//				// If folder is found, return its ID
//				if (name.equals(folderToFind)) {
//					return id;
//				}
//			}
//		}
//
//		// If folder is not found, create it and return its ID
//		return createFolderGDrive.createFolder(folderToFind, rootFolderId);
//	}
//
//}
