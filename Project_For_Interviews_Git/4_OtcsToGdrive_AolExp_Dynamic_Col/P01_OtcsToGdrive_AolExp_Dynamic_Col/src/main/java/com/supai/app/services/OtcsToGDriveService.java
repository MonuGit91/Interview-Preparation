package com.supai.app.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.supai.app.config.Otcs;
import com.supai.app.config.OtcsCategory;
import com.supai.app.dao.DocumentService;
import com.supai.app.dao.dbaction.DbOperation;
import com.supai.app.dao.dbaction.DbOperationCommon;
import com.supai.app.modal.Document;
import com.supai.app.services.common.LogBuffer;
import com.supai.app.services.common.LogUtils;
import com.supai.app.services.common.MyUtil;
import com.supai.app.services.common.SequentialLogger;
import com.supai.app.services.gdrive.api.CreateFolderGDrive;
import com.supai.app.services.gdrive.api.IsDocPresentGDrive;
import com.supai.app.services.gdrive.api.ListSubFilderGdrive;
import com.supai.app.services.gdrive.api.UploadFileContentGDrive;
import com.supai.app.services.otcsapi.CatogeryApi;
import com.supai.app.services.otcsapi.DownloadDoc;
import com.supai.app.services.otcsapi.MembersApi;
import com.supai.app.services.otcsapi.NodeApi;
import com.supai.app.services.otcsapi.SearchDocs;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtcsToGDriveService {
	private static final Logger log = LoggerFactory.getLogger(OtcsToGDriveService.class);

	private final SearchDocs searchDocs;
	private final DownloadDoc downloadDoc;
	private final Environment environment;
	private final OtcsCategory otcsCategory;
	private final Otcs otcs;
	private final CatogeryApi categoryApi;
	private final NodeApi nodeApi;
	private final MembersApi membersApi;
	private final CreateFolderGDrive createFolderGDrive;
	private final IsDocPresentGDrive isDocPresentGDrive;
	private final ListSubFilderGdrive listSubFilderGdrive;
	private final UploadFileContentGDrive uploadFileContentGDrive;
	private final SequentialLogger sequentialLogger;
	private final DocumentService documentService;
	private final MyUtil myUtil;
	
	private final DbOperation dbOperation;
	private final DbOperationCommon dbOperationCommon;

	private String otcsRootFolderName;
	private final AtomicInteger docNumber = new AtomicInteger(0);
	private static StringBuilder csvContent = new StringBuilder();
	private static StringBuilder csvHeading = new StringBuilder();

	private final ExecutorService executorService = Executors.newFixedThreadPool(30);
	private final ConcurrentHashMap<String, String> folderIdCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Object> folderLocks = new ConcurrentHashMap<>();
	private final List<Document> docList = Collections.synchronizedList(new ArrayList<>());
	private final List<JsonNode> jsonMetadataList = Collections.synchronizedList(new ArrayList<>());

	public void exportOtcsToGDrive() {
		dbOperationCommon.isColumnCorrect(otcsCategory.getAllLeafKeys());
		String otcsRootFolderId = environment.getProperty("otcs.root-folder-id");
		otcsRootFolderName = nodeApi.getNodesProperty(otcsRootFolderId).path("data").path("properties").path("name")
				.asText();

		log.info("Please Wait..");
		sequentialLogger.start();
		folderIterator(otcsRootFolderId, otcsRootFolderName + "/", "0");
		executorService.shutdown(); // Prevent new tasks

		while (!executorService.isTerminated()) {
			try {
				Thread.sleep(1000); // Check every 1 second
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.info("Interrupted while waiting.");
				break;
			}
		}

		log.info("===> Total Number of documents: {}", docNumber.get());

		if (!csvContent.isEmpty()) {
			log.info("Please wait uploading metadata to Gdrive and Database...");

			// Upload metadata.csv to Gdrive
			ResponseEntity<byte[]> metadataResponseEntity = buildMetadateResponseEntity();
			ResponseEntity<JsonNode> response = createFolderAndUploadDocToGDrive(metadataResponseEntity,
					otcsRootFolderName.split("/"), "123456", "metadata.csv");
			
			// Adding metadata to Database
//			documentService.saveAllDocsOrUpdateRow(docList).size(); // adding metadata to Database
			dbOperation.addDataToTable(jsonMetadataList);
			log.info("===> Total {} docs uploaded to Gdrive.", docList.size());
		} else {
			log.info("===> No new document found for uploading to Gdrive and Database.");
		}

		sequentialLogger.log(LogBuffer.get());// -->
		sequentialLogger.stop();
	}


	private void folderIterator(String parentId, String parentFolderPath, String type) {
		try {
			JsonNode jsonNode = searchDocs.getChildNodes(parentId);

			for (JsonNode node : jsonNode) {
				JsonNode properties = node.path("data").path("properties");
				type = properties.path("type").toString();
				String id = properties.path("id").asText();
				String name = properties.path("name").asText();

				if (otcs.getParentTypes().containsKey(type)) {
					folderIterator(id, parentFolderPath + name + "/", type);
				} else if (type.equals("144")) {
					executorService.submit(() -> processDocument(id, name, parentFolderPath));
				}
			}
		} catch (Exception e) {
			log.info("Exception while iterating OTCS folder: {}", e.getMessage());
		}
	}

	private void processDocument(String id, String name, String parentFolderPath) {
		String currentDocNumber = docNumber.getAndIncrement() + "";
		LogBuffer.append(LogUtils.info("---------------------Document start-----------------", currentDocNumber));
		LogBuffer.append(LogUtils.info("name: {}", name));
		LogBuffer.append(LogUtils.info("id: {}", id));

		try {
			JsonNode categoryDetails = categoryApi.getCatogeryDetailsById(id);
			if (categoryDetails == null) {
				LogBuffer.append(LogUtils.info("Skipping document {} because category details not found", name));
				return;
			}

			JsonNode propertyDetails = nodeApi.getNodesProperty(id);
			JsonNode userDetails = membersApi
					.getUserInfo(propertyDetails.path("data").path("properties").path("create_user_id").asText());
			JsonNode metadata = filterMetadata(categoryDetails, propertyDetails, userDetails, parentFolderPath, name,
					null, null);

			ResponseEntity<byte[]> docResponse = downloadDoc.GetDoc(id, name);

			if (docResponse == null || metadata == null) {

				LogBuffer.append(LogUtils.info("Skipping document {} because docResponse or metadata is null", name));
				return;
			}

			ResponseEntity<JsonNode> response = createFolderAndUploadDocToGDrive(docResponse,
					parentFolderPath.split("/"), id, name);
			if (response != null) {
				String docIdOnDrive = response.getBody().path("id").asText();
				String docExtention = myUtil.getFileExtension(name);
				if (!docExtention.equals("docx"))
					docExtention = "other";
				String gdriveNewDocUrl = environment.getProperty("gdrive." + docExtention).replace("{id}",
						"" + docIdOnDrive);
				if (metadata instanceof ObjectNode) {
					ObjectNode objectNode = (ObjectNode) metadata;
					objectNode = objectNode.put("Link", gdriveNewDocUrl);
				}

				synchronized (csvContent) {
					createMetadataContent(metadata, otcsRootFolderName, "metadata.csv");
				}
				docList.add(new Document(metadata));
				jsonMetadataList.add(metadata);
			}

		} catch (Exception ex) {
			LogBuffer.append(LogUtils.error("Exception processing document {}: {}", name, ex.getMessage()));
		} finally {
			LogBuffer
					.append(LogUtils.info("---------------------Document  end---------------------", currentDocNumber));
			sequentialLogger.log(LogBuffer.get());
//			log.info(LogBuffer.get());
			LogBuffer.clear();
		}
	}

	private JsonNode filterMetadata(JsonNode categoryDetails, JsonNode propertyDetails, JsonNode userDetails,
			String parentIdPath, String name, Map<String, Object> categoryFields, ObjectNode finalJson) {
		// TODO Auto-generated method stub
		try {
			final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
			JsonNode categoriesNode = categoryDetails.path("data").path("categories");
			JsonNode propertiesNode = propertyDetails.path("data").path("properties");
			JsonNode userNode = userDetails.path("data").path("properties");
			Map<String, String> userFeildNames = otcsCategory.getUserFeildNames();
			ObjectMapper objectMapper = new ObjectMapper();

			finalJson = finalJson != null ? finalJson : objectMapper.createObjectNode();
			categoryFields = categoryFields != null ? categoryFields : otcsCategory.getFeildNames();
			for (Map.Entry<String, Object> entry : categoryFields.entrySet()) {
				String categoryFieldNameYML = entry.getKey().replaceAll("[^a-zA-Z0-9_]", "");
				Object entryValue = entry.getValue();

				if (categoryFieldNameYML.equalsIgnoreCase("Name")) {
					finalJson.set(categoryFieldNameYML, new TextNode(name));
				} else if (categoryFieldNameYML.equalsIgnoreCase("Doc_Id")) {
					finalJson.set(categoryFieldNameYML, propertiesNode.path("id"));
				} else if (categoryFieldNameYML.equalsIgnoreCase("Path")) {
					finalJson.set(categoryFieldNameYML, objectMapper
							.valueToTree(Paths.get(BASE_DIRECTORY, parentIdPath, name).toString().replace("\\", "/")));
				} else if (categoryFieldNameYML.equalsIgnoreCase("Created_By")) {
					finalJson.set(categoryFieldNameYML, userNode.path("name"));
				} else if (categoryFieldNameYML.equalsIgnoreCase("Created_On")) {
					finalJson.set(categoryFieldNameYML, propertiesNode.path("create_date"));
				} else if (categoryFieldNameYML.equalsIgnoreCase("Parent_Id")) {
					try {
						finalJson.set(categoryFieldNameYML, documentService.getParentId(propertiesNode.path("id").asText()));
					} catch (Exception e) {
						finalJson.set(categoryFieldNameYML, null);
					}
				} else if (entryValue instanceof Map) {
					filterMetadata(categoryDetails, propertyDetails, userDetails, parentIdPath, name,
							(Map<String, Object>) entryValue, finalJson);
				} else if (entryValue instanceof String) {
					String categoryFieldKey = (String) entryValue;
					boolean isUser = false;
					if (userFeildNames.containsKey(categoryFieldNameYML)) {
						String userId = categoriesNode.path(categoryFieldKey).asText();
						JsonNode userName = membersApi.getUserInfo(userId, new StringBuilder()).path("data")
								.path("properties").path("name");
						finalJson.set(categoryFieldNameYML, userName);
						isUser = true;
					}
					if (!isUser) {
						if (!categoryFieldKey.contains("x")) {
							finalJson.set(categoryFieldNameYML, categoriesNode.path(categoryFieldKey));
						} else {
							List<String> categoryValues = new ArrayList<>();
							String feildId = categoryFieldKey.replace("x", "1");
							for (int rowNum = 1; !categoriesNode.path(feildId).isMissingNode(); rowNum++) {
								feildId = categoryFieldKey.replace("x", "" + rowNum);
								String feildValue = categoriesNode.path(feildId).asText().trim();
								categoryValues.add(feildValue);
							}
							categoryValues.remove(categoryValues.size() - 1); // because at the end one empty feild is
																				// added
							String result = categoryValues.toString().replace(",", " | ");
							finalJson.set(categoryFieldNameYML, TextNode.valueOf(result));
						}

					}
				}

			}

			return finalJson;
		} catch (Exception e) {
			LogBuffer.append(LogUtils.error("Error: Exception while creating finla metadata - {}", e.getMessage()));
		}

		return null;
	}

	private ResponseEntity<byte[]> buildMetadateResponseEntity() {
		String finalCsv = csvHeading.toString() + "\n" + csvContent.toString();
		byte[] csvBytes = finalCsv.getBytes(StandardCharsets.UTF_8);

		// Step 3: Prepare fake ResponseEntity<byte[]> (mimicking downloaded file)
		ResponseEntity<byte[]> csvEntity = new ResponseEntity<>(csvBytes, HttpStatus.OK);
		return csvEntity;
	}

	private void createMetadataContent(JsonNode metadata, String filename, String folderId) {
		StringBuilder currentCsvHeading = new StringBuilder();
		StringBuilder currentCsvContent = new StringBuilder();

		Iterator<Map.Entry<String, JsonNode>> fields = metadata.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			currentCsvHeading.append(entry.getKey()).append(",");
			String entryValue = entry.getValue().toString().replace(",", " | ").replace("null", "");
			currentCsvContent.append(entryValue).append(",");
		}
		if (csvHeading.isEmpty())
			csvHeading = currentCsvHeading;
		csvContent.append(currentCsvContent).append("\n");

	}

	public void exportMetadataToRelativeFolder(JsonNode metadata, String relativeExpoertPaht, String filename) {
		try {
			final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
			Path csvFilePath = Paths.get(BASE_DIRECTORY, otcsRootFolderName, "metadata.csv");
			boolean fileExists = Files.exists(csvFilePath);

			StringBuilder csvContent = new StringBuilder();
			StringBuilder csvHeadings = new StringBuilder();

			Iterator<Map.Entry<String, JsonNode>> fields = metadata.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				if (!fileExists)
					csvHeadings.append(entry.getKey()).append(",");
				String entryValue = entry.getValue().toString().replace(",", " | ").replace("null", "");
				csvContent.append(entryValue).append(",");
			}

			if (!fileExists)
				Files.write(csvFilePath, csvHeadings.append("\n").toString().getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);
			Files.write(csvFilePath, csvContent.append("\n").toString().getBytes(), StandardOpenOption.APPEND);

		} catch (Exception e) {
			log.info("X Error writing CSV file: ");
			e.printStackTrace();
		}
	}

	private void saveFile(String folderPath, String fileName, byte[] fileData) {
		final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
		Path filePath = null;
		try {
			filePath = Paths.get(BASE_DIRECTORY, folderPath, fileName);
			Files.write(filePath, fileData);
		} catch (Exception e) {
			log.info("X Error Saving File: " + filePath.toString());
			e.printStackTrace();
		}
	}

	private boolean createFolder(String folderPath) {
		final String BASE_DIRECTORY = environment.getProperty("local.root-folder");
		try {
			Path localPath = Paths.get(BASE_DIRECTORY, folderPath);
			Files.createDirectories(localPath);
		} catch (Exception e) {
			log.info("X Error Creating Folder: " + folderPath);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private ResponseEntity<JsonNode> createFolderAndUploadDocToGDrive(ResponseEntity<byte[]> responseDoc,
			String[] parentFolderPathArr, String docId, String docName) {
		LogBuffer.append(LogUtils.info("Uploading document to Gdrive..."));
		// Get root folder ID from environment and search for or create the spend
		String rootFolderIdInGdrive = environment.getProperty("gdrive.folder.root");
		String parentFolderId = "";
		String parentFolder = parentFolderPathArr[parentFolderPathArr.length - 1];
		for (int i = 0; i < parentFolderPathArr.length; i++) {
//			parentFolderId = getFolderIdFromGdrive(parentFolderPathArr[i],
//					parentFolderId.isEmpty() ? rootFolderIdInGdrive : parentFolderId);
			parentFolderId = getFolderIdFromGdriveSafely(parentFolderPathArr[i],
					parentFolderId.isEmpty() ? rootFolderIdInGdrive : parentFolderId);
		}

		if (parentFolder == null) {
			LogBuffer.append(LogUtils.error("Error: Can not upload to GDrive because Parent Folder is null!"));
			return null;
		}

		// Check if the document is already present in Google Drive to avoid duplicates
		String correctParentFolderId = parentFolderId;
		ResponseEntity<String> responseIsDocExist = isDocPresentGDrive.isDocPresentOnGdrive(docName,
				correctParentFolderId);
		boolean isDocExist = ((responseIsDocExist == null) || (responseIsDocExist.getBody() == null)
				|| responseIsDocExist.getBody().contains(docName));

		if (isDocExist) {// This if block update the name of doc and upload to gdrive.
			if (docName == "metadata.csv" && responseDoc != null) {
				// This block will run only when doc name is matadata.csv is present on gdrive.
				// updateName Method will update the doc name like
				// oldName_<date>_<time>.<extention>
				String newDocName = updateName(docName);
				uploadFileContentGDrive.uploadToGdrive(responseDoc, parentFolder, newDocName, docId,
						rootFolderIdInGdrive, correctParentFolderId);
			} else {
				LogBuffer
						.append(LogUtils.info("Skipping uploading to GDrive because it is already present on Gdrive."));
			}
		} else if (responseDoc != null) {
			// If the document does not exist in Google Drive, then upload it to Google
			// Drive
			return uploadFileContentGDrive.uploadToGdrive(responseDoc, parentFolder, docName, docId,
					rootFolderIdInGdrive, correctParentFolderId);
		}
		return null;

	}

	public void upload() {
		// Attempt to search for all documents in OTCS
		JsonNode resultsNode = searchDocs.getDocs();

		if (resultsNode == null || !resultsNode.isArray()) {
			log.info("Faild: Documents Searching Faild");
			return;
		}

		log.info(resultsNode.size() + " documents found");

		// Iterate over each result document in the search results array
		int docNumber = 1;
		for (JsonNode result : resultsNode) {
			String docId = result.path("data").path("properties").path("id").asText();
			String docName = result.path("data").path("properties").path("name").asText();
			String spendCategory = result.path("data").path("regions").path(otcsCategory.getAttr()).asText();

			// Check if any critical field is empty
			if (docId.isEmpty() || docName.isEmpty() || spendCategory.isEmpty()) {
				if (docId.isEmpty()) {
					log.info("Failed: Document ID is missing or empty");
				}
				if (docName.isEmpty()) {
					log.info("Failed: Document Name is missing or empty");
				}
				if (spendCategory.isEmpty()) {
					log.info("Failed: Spend Category is missing or empty");
				}
				continue;
			}

			// Check that document ID, name, and spend category are non-empty
			log.info("---------------------Document {} start---------------------", docNumber);
			log.info("Uploading document having docId: {}, name: {}, spendCategory: {}", docId, docName, spendCategory);

			// Get root folder ID from environment and search for or create the spend
			String rootFolderIdInGdrive = environment.getProperty("gdrive.folder.root");
			String spendCategoryIdInGdrive = getFolderIdFromGdrive(spendCategory, rootFolderIdInGdrive);

			if (spendCategoryIdInGdrive == null) {
				log.info("---------------------Document {} End ---------------------\n", docNumber++);
				return;
			}

			// Check if the document is already present in Google Drive to avoid duplicates
			ResponseEntity<String> responseIsDocExist = isDocPresentGDrive.isDocPresentOnGdrive(docName,
					spendCategoryIdInGdrive);
			boolean isDocExist = ((responseIsDocExist == null) || (responseIsDocExist.getBody() == null)
					|| responseIsDocExist.getBody().contains(docName));

			// If the document does not exist in Google Drive, download it from OTCS and
			// upload it to Google Drive
			if (isDocExist) {
				String newDocName = updateName(docName);
				ResponseEntity<byte[]> responseDoc = downloadDoc.GetDoc(docId, docName);
				if (responseDoc != null) {
					uploadFileContentGDrive.uploadToGdrive(responseDoc, spendCategory, newDocName, docId,
							rootFolderIdInGdrive, spendCategoryIdInGdrive);
				}
			} else {
				ResponseEntity<byte[]> responseDoc = downloadDoc.GetDoc(docId, docName);
				if (responseDoc != null) {
					uploadFileContentGDrive.uploadToGdrive(responseDoc, spendCategory, docName, docId,
							rootFolderIdInGdrive, spendCategoryIdInGdrive);
				}
			}
			log.info("---------------------Document {} End ---------------------\n", docNumber++);

		}
	}

	private String updateName(String docName) {
		// Date and time format: ddMMyyyy_HHmmss
		String originalFileName = docName; // Example without extension

		// Date and time format: ddMMyyyy_HHmmss
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");

		// Get the current date and time from the server
		LocalDateTime now = LocalDateTime.now();

		// Format the current date and time
		String formattedDateTime = now.format(formatter);

		// Check if the file has an extension
		String name;
		String extension = "";
		int lastDotIndex = originalFileName.lastIndexOf('.');

		if (lastDotIndex != -1) {
			// File has an extension
			name = originalFileName.substring(0, lastDotIndex); // Name without extension
			extension = originalFileName.substring(lastDotIndex); // Extension with dot
		} else {
			// File has no extension
			name = originalFileName;
		}

		// Append the formatted date and time between name and extension
		String finalFileName = name + "_" + formattedDateTime + extension;

		return finalFileName;
	}

	private String getFolderIdFromGdriveSafely(String folderToFind, String rootFolderId) {
		if (folderToFind == null || rootFolderId == null) {
			log.warn("folderToFind: {} | rootFolderId: {}", folderToFind, rootFolderId);
			return null;
		}
		// First: quick check if we already know the folderId
		if (folderIdCache.containsKey(folderToFind)) {
			return folderIdCache.get(folderToFind);
		}

		// Get lock for this folder path
		Object lock = folderLocks.computeIfAbsent(folderToFind, k -> new Object());

		// Lock only folder creation section
		synchronized (lock) {
			// Double-check to avoid duplicate creation
			if (folderIdCache.containsKey(folderToFind)) {
				return folderIdCache.get(folderToFind);
			}

			// 1. Check if folder exists in Drive
			String folderId = getFolderIdFromGdrive(folderToFind, rootFolderId);

			// 3. Store in cache for reuse
			folderIdCache.put(folderToFind, folderId);

			return folderId;
		}
	}

	private String getFolderIdFromGdrive(String folderToFind, String rootFolderId) {
		if (folderToFind == null || rootFolderId == null) {
			return null;
		}

		// Fetch the list of subfolders under the root folder to check for the specified
		// folder
		JsonNode subFolders = listSubFilderGdrive.fetchSubFolders(rootFolderId);
		if (subFolders == null || subFolders.has("error")) {
			return null;
		}

		JsonNode filesNode = subFolders.get("files");
		if (filesNode.isArray()) {
			for (JsonNode fileNode : filesNode) {
				String id = fileNode.get("id").asText();
				String name = fileNode.get("name").asText();
				// If folder is found, return its ID
				if (name.equals(folderToFind)) {
					return id;
				}
			}
		}

		// If folder is not found, create it and return its ID
		return createFolderGDrive.createFolder(folderToFind, rootFolderId);
	}

}
