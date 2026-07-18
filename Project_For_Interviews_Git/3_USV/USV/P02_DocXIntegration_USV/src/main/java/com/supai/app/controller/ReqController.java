package com.supai.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.dao.dto.DocumentRequest;
import com.supai.app.dao.dto.VersionRequest;
import com.supai.app.services.UsvService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // allow all origins (or specify your frontend URL)
public class ReqController {
//	private final UsvService usvService;
	private final UsvService usvService;

	@PostMapping(value = "/gdoc/edit", consumes = "application/json", produces = "application/json")
	public ResponseEntity<JsonNode> getDocumentById(@RequestBody DocumentRequest request) {
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(request.getOtcsDocId());
		log.info("POST /gdoc/edit hitted!");

//		ResponseEntity<JsonNode> response = otcsToGDriveNewService.exportOtcsToGDrive(request);
		ResponseEntity<JsonNode> response = usvService.DocExportOtcsToGDrive(request);

		log.info("Response Body: {}", response.getBody());
		Thread.currentThread().setName(originalThreadName);
		return response;
	}

	@PostMapping(value = "/gdoc/addVersion", consumes = "application/json", produces = "application/json")
	public ResponseEntity<String> addVersion(@RequestBody VersionRequest request) {
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(request.getOtcsDocId());
		log.info("POST /gdoc/addVersion hitted!");
		ResponseEntity<String> response = usvService.addVersionToDoc(request);
		Thread.currentThread().setName(originalThreadName);
		return response;
	}

}