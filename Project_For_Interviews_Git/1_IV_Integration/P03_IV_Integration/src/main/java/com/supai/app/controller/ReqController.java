package com.supai.app.controller;

import java.io.IOException;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.constants.Rock;
import com.supai.app.dto.request.CustomePageRequest;
import com.supai.app.dto.request.DeleteReq;
import com.supai.app.dto.request.GraphQlApiPojo;
import com.supai.app.dto.request.MergeRequest;
import com.supai.app.dto.request.PdfRequest;
import com.supai.app.dto.request.PdfRequest1;
import com.supai.app.dto.request.ToPdfRequest;
import com.supai.app.ivapis.dto.response.IvTicketResponse;
import com.supai.app.otcsapis.CreateOrCopyNode;
import com.supai.app.otcsapis.dto.request.CreateOrCopyNodeRequestDto;
import com.supai.app.otcsapis.dto.response.CreateOrCopyNodeResponseDto;
import com.supai.app.otds.OtdsToken;
import com.supai.app.services.DeleteService;
import com.supai.app.services.PdfService;
import com.supai.app.services.PdfService1;
import com.supai.app.services.ToPdfSercice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // allow all origins (or specify your frontend URL)
public class ReqController {
	private final ToPdfSercice toPdfSercice;
	private final PdfService pdfService;
	private final DeleteService deleteService;
	private final PdfService1 pdfService1;
	private final ObjectMapper objectMapper;
	private final CreateOrCopyNode createOrCopyNode;
	private final OtdsToken otdsToken;

//	@PostMapping("/pdf")
//	public JsonNode convertToPDF(@RequestBody ToPdfRequest request) throws IOException {
//		return toPdfSercice.pdfAgent(request);
//	}
//
//	@PostMapping("/pdf/banner/top")
//	public JsonNode convertToPDF(@RequestBody TopStampRequest request) throws IOException {
//		return toPdfSercice.pdfBannerAgent(request);
//	}
//
//	@PostMapping("/pdf/banner/issuance")
//	public JsonNode convertToStampedPDF(@RequestBody @Valid StampRequest request) throws IOException {
//		return toPdfSercice.pdfStampAgent(request);
//	}
//
//	@PostMapping("/copy")
//	public JsonNode copy(@RequestBody CreateOrCopyNodeRequestDto request) throws IOException{
//		CreateOrCopyNodeResponseDto responseDto = createOrCopyNode.copyNode(otdsToken.getOtdsToken().getTicket(), request);
//		return objectMapper.valueToTree(responseDto);
//	}
	
	@DeleteMapping("/version/delete")
	public void deleteVersion(@RequestBody DeleteReq deleteReq) throws Exception {
		log.info(Rock.LineStart);
		try {
			log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deleteReq));
		} catch(Exception e) {
			log.error("/version/delete \n{}", e.getMessage());
			throw new RuntimeException("Not able to parse Json provided while calling Post: /version/delete");
		}
		deleteService.deleteAgent(deleteReq);
	}

	@PostMapping("/token/bearer")
	public IvTicketResponse bearer(@RequestBody ToPdfRequest request) {
		log.info(Rock.LineStart);

		try {
			log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
		} catch(Exception e) {
			log.error("/token/bearer \n{}", e.getMessage());
			throw new RuntimeException("Not able to parse Json provided while calling Post: /token/bearer");
		}
		return toPdfSercice.getBearertoken(request);
	}
	
	@PostMapping("/pdf/merge")
	public JsonNode mergeToPdf(@RequestBody @Valid MergeRequest mergeRequest) {
		log.info(Rock.LineStart);

		try {
			log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mergeRequest));
		} catch(Exception e) {
			log.error("/pdf/merge \n{}", e.getMessage());
			throw new RuntimeException("Not able to parse provided json");
		}
		JsonNode response =  toPdfSercice.mergePdfAgent(mergeRequest);
		return response;
	}

	@PostMapping("/pdf/banner")
	public JsonNode convertToPDF(@RequestBody PdfRequest request) throws IOException {
		log.info(Rock.LineStart);

		try {
			log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
		} catch(Exception e) {
			log.error("/pdf/banner \n{}", e.getMessage());
			throw new RuntimeException("Not able to parse Json provided while calling Post: /pdf/banner");
		}
		return pdfService.pdfBannerAgent(request);
	}

	@PostMapping("/pdf/banner1")
	public JsonNode convertToPDF1(@RequestBody PdfRequest1 request) throws IOException {
		log.info(Rock.LineStart);

		try {
			log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
		} catch(Exception e) {
			log.error("/pdf/banner1 \n{}", e.getMessage());
			throw new RuntimeException("Not able to parse Json provided while calling Post: /pdf/banner1");
		}
		return pdfService1.pdfBannerAgent(request);
	}

	@PostMapping("/pdf")
	public JsonNode convertToPDF1(@RequestBody CustomePageRequest request) throws Exception {
		log.info(Rock.LineStart);

		try {
			log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
		} catch(Exception e) {
			log.error("/pdf \n{}", e.getMessage());
			throw new RuntimeException("Not able to parse Json provided while calling Post: /pdf");
		}
		return pdfService1.publishDocWithCustomePage(request);
//		byte[] pdfBytes = pdfService1.publishDocWithCustomePage(request);
//		return ResponseEntity.ok()
//				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"")
//				.contentType(MediaType.APPLICATION_PDF)
//				.body(pdfBytes);
	}
	
	@PostMapping("/qr/attach")
	public JsonNode attachQr(@RequestBody GraphQlApiPojo graphQlPojo) {
		log.info(Rock.LineStart);
		try {
			log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graphQlPojo));
		} catch(Exception e) {
			log.error("/qr/attach \n{}", e.getMessage());
			throw new RuntimeException("Not able to parse Json provided while calling Post: /qr/attach");
		}
		return toPdfSercice.applyQr(graphQlPojo);
	}
}