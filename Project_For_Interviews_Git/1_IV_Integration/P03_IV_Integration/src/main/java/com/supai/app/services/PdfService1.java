package com.supai.app.services;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.supai.app.config.OtcsApi;
import com.supai.app.dto.request.CustomePageRequest;
import com.supai.app.dto.request.GraphQlApiPojo;
import com.supai.app.dto.request.PdfRequest1;
import com.supai.app.dto.request.ToPdfRequest;
import com.supai.app.ivapis.DownloadArtifact;
import com.supai.app.ivapis.IVTicket;
import com.supai.app.ivapis.Publication;
import com.supai.app.ivapis.PublicationDetails;
import com.supai.app.ivapis.PublicationStatus;
import com.supai.app.ivapis.dto.response.IvTicketResponse;
import com.supai.app.ivapis.dto.xml.PageNoBanners;
import com.supai.app.ivapis.dto.xml.XmlBanner1;
import com.supai.app.ivapis.dto.xml.XmlBanners1Json;
import com.supai.app.ivapis.dto.xml.XmlObjFactory;
import com.supai.app.otcsapis.AddVersion;
import com.supai.app.otcsapis.AllVersions;
import com.supai.app.otcsapis.OtcsToken;
import com.supai.app.otcsapis.dto.response.OtdsTokenResponseDto;
import com.supai.app.otds.OtdsToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfService1 {
	private final XmlObjFactory xmlObjFactory;
	private final XmlMapper xmlMapper;
//	private final ImpersonateUser impersonateUser;
	private final AllVersions allVersions;
	private final IVTicket iVTicket;
	private final Publication publication;
	private final DownloadArtifact downloadArtifact;
	private final AddVersion addVersion;
	private final PublicationStatus publicationStatus;
	private final PublicationDetails publicationDetails;
	private final OtcsApi otcsApi;
	private final OtcsToken otcsToken;
	private final OtdsToken otdsToken;

	public JsonNode pdfBannerAgent(PdfRequest1 pdfRequest1) throws JsonProcessingException {
		// TODO Auto-generated method stub
		ToPdfRequest request = pdfRequest1.getPdfRequest();
		XmlBanners1Json xmlBanners1Json = pdfRequest1.getBanner1Json();

		String xmlContent = getXmlContent(xmlBanners1Json, pdfRequest1.getPdfRequest().getIncludePageNo());
		String base64EncodedXML = xmlObjFactory.xmlBase64Incoded(xmlContent);

//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);
		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTokenResponseDto.getTicket(),
				request.getNodeId(),
				maxVersionNo);

		// String bearerToken = itcketNode.path("token").asText();

		JsonNode publicationNode = publication.callPdfPublicatinApi_(request.getNodeId(), ivTicketResponse,
				maxVersionNo, false, base64EncodedXML);

		log.info("Publication Response: {}", publicationNode.asText());

		if (publicationNode == null || !publicationNode.has("id")) {
			log.error("Publication API did not return an ID. Response: {}", publicationNode);
			throw new RuntimeException("Publication API failed: Missing 'id' in response");
		}
		String id = publicationNode.get("id").asText();

		checkStatus(id, ivTicketResponse);

		byte[] fileData = downloadArtifact.downloadDoc(id, ivTicketResponse);
		
		String otdsTicket = otdsTokenResponseDto.getTicket();
		
		JsonNode response = addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otdsTicket, fileData,
				request.getFinalDocName());
		log.info(response.toPrettyString());
		return response;
	}

	public String getXmlContent(XmlBanners1Json xmlBanners1Json, boolean includePageNo) {
		String topCenter = getContentOfLocation(xmlBanners1Json, "TopCenter");
		String bottomLeft = getContentOfLocation(xmlBanners1Json, "BottomLeft");
		String bottomCenter = getContentOfLocation(xmlBanners1Json, "BottomCenter");
		String bottomRight = getContentOfLocation(xmlBanners1Json, "BottomRight");

		String color = (xmlBanners1Json == null || xmlBanners1Json.getColor() == null
				|| xmlBanners1Json.getColor().isEmpty() ? "#000000" : xmlBanners1Json.getColor());

		// boolean includePageNo = pdfRequest1.getPdfRequest().getIncludePageNo();

		
		XmlBanner1 xmlBanner = xmlObjFactory.createXmlBanner1(topCenter, bottomLeft, bottomCenter, bottomRight, color,
				"", xmlBanners1Json.getFontHeight(), includePageNo);

		try {
			String xmlContent = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(xmlBanner);
			// Post-process to ensure numeric entities like &#10; are not double-escaped as
			// &amp;#10;
			xmlContent = xmlContent.replaceAll("&amp;#", "&#");
			return xmlContent;
		} catch (Exception e) {
			log.error(e.toString());
			throw new RuntimeException(e.getMessage());
		}

	}

	public JsonNode publishDocWithCustomePage(CustomePageRequest request) throws Exception {

		// XmlBanners1Json xmlBanners1Json = xmlObjFactory.createXmlBanners1Json();
		// String xmlContent = getXmlContent(xmlBanners1Json,
		// request.getIncludePageNo());
		// String base64EncodedXML = xmlObjFactory.xmlBase64Incoded(xmlContent);

//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);
		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTicket,
				request.getNodeId(),
				maxVersionNo);

		JsonNode pageDetails = checkPageStatus(ivTicketResponse);

		int pageCount = pageDetails
				.at("/_embedded/pa:get_publication_artifacts/0/_embedded/ac:get_artifact_content/content/pageCount")
				.asInt();
		log.info("Page Count: {}", pageCount);
		PageNoBanners pageNoBanners = PageNoBanners.generateForPageCount(pageCount);
		String pageNoBannersXml = pageNoBanners.toXmlString(xmlMapper);
		String base64EncodedXML = xmlObjFactory.xmlBase64Incoded(pageNoBannersXml);

		JsonNode publicationNode = publication.customePagePublication(request.getNodeId(), ivTicketResponse,
				maxVersionNo, null, // url is now fetched internally in Publication.java
				base64EncodedXML, request.getPageRange(), pageCount);


		log.info("Publication Response: {}", publicationNode != null ? publicationNode.toPrettyString() : "null");

		if (publicationNode == null || !publicationNode.has("id")) {
			log.error("Publication API did not return an ID. Response: {}", publicationNode);
			throw new RuntimeException("Publication API failed: Missing 'id' in response");
		}
		String id = publicationNode.get("id").asText();

		checkStatus(id, ivTicketResponse);

		byte[] fileData = downloadArtifact.downloadDoc(id, ivTicketResponse);
		JsonNode response = addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otdsTicket, fileData,
				"new");
		log.info(response.toPrettyString());
		return response;
	}

	public JsonNode qrAttachmentService(GraphQlApiPojo request) {

		// XmlBanners1Json xmlBanners1Json = xmlObjFactory.createXmlBanners1Json();
		// String xmlContent = getXmlContent(xmlBanners1Json,
		// request.getIncludePageNo());
		// String base64EncodedXML = xmlObjFactory.xmlBase64Incoded(xmlContent);

		String userName = request.getVariables().getMarkups().get(0).getAuthor();

//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);
		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTicket,
				request.getNodeId(),
				maxVersionNo);

		JsonNode pageDetails = checkPageStatus(ivTicketResponse);
		int pageCount = pageDetails
				.at("/_embedded/pa:get_publication_artifacts/0/_embedded/ac:get_artifact_content/content/pageCount")
				.asInt();
		log.info("Page Count: {}", pageCount);
		
		//for qr code no need to applying banner so passing page no as 0
//		PageNoBanners pageNoBanners = PageNoBanners.generateForPageCount(pageCount);
		PageNoBanners pageNoBanners = PageNoBanners.generateForPageCount(0);
		String pageNoBannersXml = pageNoBanners.toXmlString(xmlMapper);
		String base64EncodedXML = xmlObjFactory.xmlBase64Incoded(pageNoBannersXml);

		// String bearerToken = itcketNode.path("token").asText();

		JsonNode publicationNode = publication.applyQrPublication(request.getNodeId(), ivTicketResponse, maxVersionNo,
				base64EncodedXML, request.getPageRange());

		// log.info("Publication Response: {}", publicationNode != null ?
		// publicationNode.toPrettyString() : "null");

		if (publicationNode == null || !publicationNode.has("id")) {
			log.error("Publication API did not return an ID. Response: {}", publicationNode);
			throw new RuntimeException("Publication API failed: Missing 'id' in response");
		}
		String id = publicationNode.get("id").asText();

		// byte[] fileData = downloadArtifact.downloadDoc(id, ivTicketResponse);
		// return fileData;
		return checkStatus(id, ivTicketResponse);
	}

	public JsonNode checkPageStatus(IvTicketResponse ivTicketResponse) {
		JsonNode statusNode = publicationDetails.callPageDetails(ivTicketResponse);
		String status = statusNode.get("status").asText();
		while (true) {
			if (status.equals("Complete"))
				break;

			try {
				Thread.sleep(1000 * 2);
				statusNode = publicationDetails.callPageDetails(ivTicketResponse);
				status = statusNode.get("status").asText();
				log.info("Current Status: {}", statusNode.get("status").asText());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return statusNode;
	}

	public JsonNode checkStatus(String id, IvTicketResponse ivTicketResponse) {
		JsonNode statusNode = publicationStatus.callPublicationStatus(id, ivTicketResponse);
		String status = statusNode.get("status").asText();
		while (true) {
			if (status.equals("Complete"))
				break;

			try {
				Thread.sleep(1000 * 2);
				statusNode = publicationStatus.callPublicationStatus(id, ivTicketResponse);
				status = statusNode.get("status").asText();
				log.info("Current Status: {}", statusNode.get("status").asText());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return statusNode;
	}

	@SuppressWarnings("unchecked")
	private String getContentOfLocation(XmlBanners1Json xmlBanners1Json, String location) {
		if (xmlBanners1Json == null) {
			return "";
		}

		XmlBanners1Json.BannerContent bannerContent = null;
		if ("TopCenter".equals(location)) {
			bannerContent = xmlBanners1Json.getTopCenter();
		} else if ("BottomLeft".equals(location)) {
			bannerContent = xmlBanners1Json.getBottomLeft();
		} else if ("BottomCenter".equals(location)) {
			bannerContent = xmlBanners1Json.getBottomCenter();
		} else if ("BottomRight".equals(location)) {
			bannerContent = xmlBanners1Json.getBottomRight();
		}

		if (bannerContent == null || bannerContent.getContent() == null) {
			return "";
		}

		Map<String, Object> contentMap = bannerContent.getContent();
		boolean horizontal = bannerContent.getHorizontal() != null && bannerContent.getHorizontal();
		boolean includesKey = bannerContent.getIncludesKey() != null && bannerContent.getIncludesKey();

		if (horizontal && includesKey) {
			return getHorizontallySeparatedMapEntry(contentMap);
		} else if (horizontal && !includesKey) {
			return getHorizontallySeparatedMapValues(contentMap);
		} else if (!horizontal && includesKey) {
			return getMapEntryString(contentMap);
		} else {
			return getMapValueOfString(contentMap);
		}
	}

	private String getMapValueOfString(Map<String, Object> contentMap) {
		// TODO Auto-generated method stub
		return contentMap.values().stream().map(v -> v == null ? "" : v.toString()).filter(v -> !v.isEmpty())
				.collect(Collectors.joining("&#10;"));
	}

	private String getMapEntryString(Map<String, Object> contentMap) {
		// TODO Auto-generated method stub
		return contentMap.entrySet().stream().map(e -> e == null ? "" : e.getKey() + " : " + e.getValue())
				.filter(e -> !e.isEmpty()).collect(Collectors.joining("&#10;"));
	}

	private String getHorizontallySeparatedMapValues(Map<String, Object> contentMap) {
		return contentMap.values().stream().map(v -> v == null ? "" : v.toString()).filter(v -> !v.isEmpty())
				.collect(Collectors.joining(" | "));
	}

	private String getHorizontallySeparatedMapEntry(Map<String, Object> contentMap) {
		return contentMap.entrySet().stream().filter(entry -> entry.getValue() != null).map(entry -> {
			return entry.getKey() + " : " + entry.getValue().toString();
		}).collect(Collectors.joining(" | "));
	}

}
