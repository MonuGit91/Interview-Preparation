//package com.supai.app.dao;
//
//import java.util.*;
//
//import org.springframework.stereotype.Component;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.TextNode;
//import com.supai.app.modal.Document;
//import com.supai.app.repository.DocumentRepository;
//import com.supai.app.repository.DtreeRepository;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class DocumentService {
////	private static final Logger log = LoggerFactory.getLogger(DocumentService.class); // @Slf4j will take care this line
//	private final DocumentRepository documentRepository;
//	private final DtreeRepository dtreeRepository;
//
//	public JsonNode getParentId(String dataId) {
//		try {
//			String parentId = dtreeRepository.getParentIdByDataId(dataId);	
//			return TextNode.valueOf(parentId);
//		} catch (Exception e) {
//			return null;
//		}
//	}
//	//This method will one by one add new record or update old record wiht new if find
//	public List<Document> saveAllDocsOrUpdateRow(List<Document> docList) {
//	    List<Document> savedDocs = new ArrayList<>();
//
//	    for (Document doc : docList) {
//	        try {
//	            Document saved = documentRepository.save(doc); // INSERT or UPDATE
//	            savedDocs.add(saved);
//	        } catch (Exception e) {
//	            log.error("Failed to save or update document with Doc_Id: {}. Error: {}", doc.getDocId(), e.getMessage(), e);
//	        }
//	    }
//
//	    log.info("===> Total {} documents inserted or updated in the database.", savedDocs.size());
//	    return savedDocs;
//	}
//
//	// This method will only insert the row with new doc id one by one
//	public List<Document> saveAllNewDocsOnly(List<Document> docList) {
//	    List<Document> savedDocs = new ArrayList<>();
//
//	    // Step 1: Extract all Doc_Ids from the input list
//	    List<String> inputIds = docList.stream()
//	                                   .map(Document::getDocId)
//	                                   .toList();
//
//	    // Step 2: Load existing Doc_Ids from DB
//	    Set<String> existingIds = new HashSet<>(
//	        documentRepository.findAllById(inputIds)
//	                          .stream()
//	                          .map(Document::getDocId)
//	                          .toList()
//	    );
//
//	    // Step 3: Save documents one-by-one, skipping duplicates and handling exceptions
//	    Set<String> skippedDocs = new HashSet<>();
//	    for (Document doc : docList) {
//	        if (existingIds.contains(doc.getDocId())) {
//	        	skippedDocs.add(doc.getDocId());
//	            continue;
//	        }
//	        try {
//	            Document saved = documentRepository.save(doc);
//	            savedDocs.add(saved);
//	        } catch (Exception e) {
//	            log.error("Failed to save document with Doc_Id: {}. Error: {}", doc.getDocId(), e.getMessage(), e);
//	        }
//	    }
//	    if(!skippedDocs.isEmpty())  log.info("===> Total {} documents added in database.", savedDocs.size());
//	    return savedDocs;
//	}
//
//	// This method will save row in database in bach
//	public List<Document> saveAllDocsBachwise(List<Document> docList) {
//		List<Document> savedDocs = new ArrayList<>();
//		int batchSize = 29;
//
//		// Step 1: Extract all Doc_Ids from the input list
//		List<String> inputIds = docList.stream().map(Document::getDocId).toList();
//
//		// Step 2: Load existing Doc_Ids from DB
//		List<String> existingIds = documentRepository.findAllById(inputIds).stream().map(Document::getDocId).toList();
//		Set<String> existingIdSet = new HashSet<>(existingIds);
//
//		// Step 3: Filter only new documents
//		List<Document> newDocs = docList.stream().filter(doc -> !existingIdSet.contains(doc.getDocId())).toList();
//
//		// Step 4: Save new documents in batches
//		for (int i = 0; i < newDocs.size(); i += batchSize) {
//			int end = Math.min(i + batchSize, newDocs.size());
//			List<Document> batch = newDocs.subList(i, end);
//
//			try {
//				List<Document> savedBatch = documentRepository.saveAll(batch);
//				savedDocs.addAll(savedBatch);
//			} catch (Exception e) {
//				log.error("Error saving batch from {} to {}: {}", i, end, e.getMessage(), e);
//			}
//		}
//
//		return savedDocs;
//	}
//
//	public List<Document> getAllDocuments() {
//		try {
//			List<Document> documents = documentRepository.findAll();
//			log.info("Fetched {} documents from database", documents.size());
//			return documents;
//		} catch (Exception e) {
//			log.error("Error fetching documents: {}", e.getMessage(), e);
//			return Collections.emptyList();
//		}
//	}
//}
