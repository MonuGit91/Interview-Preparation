package com.supai.app.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.supai.app.config.OtcsApi;
import com.supai.app.dto.request.PdfRequest;
import com.supai.app.dto.request.ToPdfRequest;
import com.supai.app.ivapis.DownloadArtifact;
import com.supai.app.ivapis.IVTicket;
import com.supai.app.ivapis.Publication;
import com.supai.app.ivapis.PublicationStatus;
import com.supai.app.ivapis.dto.response.IvTicketResponse;
import com.supai.app.ivapis.dto.xml.XmlBanner;
import com.supai.app.ivapis.dto.xml.XmlBannersJson;
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
public class PdfService {
	private final XmlObjFactory xmlObjFactory;
	private final XmlMapper xmlMapper;
	private final AllVersions allVersions;
	private final IVTicket iVTicket;
	private final Publication publication;
	private final DownloadArtifact downloadArtifact;
	private final AddVersion addVersion;
	private final PublicationStatus publicationStatus;
	private final OtcsApi otcsApi;
	private final OtcsToken otcsToken;
	private final OtdsToken otdsToken;

	public JsonNode pdfBannerAgent(PdfRequest pdfRequest) throws JsonProcessingException {
		// TODO Auto-generated method stub
		ToPdfRequest request = pdfRequest.getPdfRequest();
		XmlBannersJson xmlBannersJson = pdfRequest.getBannerJson();

		String topCenter = getContentOfLocation(xmlBannersJson, "TopCenter");
		String bottomLeft = getContentOfLocation(xmlBannersJson, "BottomLeft");
		String bottomCenter = getContentOfLocation(xmlBannersJson, "BottomCenter");
		String bottomRight = getContentOfLocation(xmlBannersJson, "BottomRight");

		String color = (xmlBannersJson == null || xmlBannersJson.getBanners() == null
				|| xmlBannersJson.getBanners().isEmpty()) ? "#000000"
						: xmlBannersJson.getBanners().get(0).getTextFragment().getColor();

		boolean includePageNo = pdfRequest.getPdfRequest().getIncludePageNo();

		XmlBanner xmlBanner = xmlObjFactory.createXmlBanner(topCenter, bottomLeft, bottomCenter, bottomRight, color,
				includePageNo);

		String xmlContent = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(xmlBanner);
		xmlContent = xmlContent.replaceAll("&amp;#", "&#");
		String base64EncodedXML = xmlObjFactory.xmlBase64Incoded(xmlContent);

//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);
		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTokenResponseDto.getTicket(), request.getNodeId(),
				maxVersionNo);

		// String bearerToken = itcketNode.path("token").asText();

		JsonNode publicationNode = publication.callPdfPublicatinApi_(request.getNodeId(), ivTicketResponse,
				maxVersionNo, false, base64EncodedXML);

		log.info("Publication Response: {}", publicationNode != null ? publicationNode.toPrettyString() : "null");

		if (publicationNode == null || !publicationNode.has("id")) {
			log.error("Publication API did not return an ID. Response: {}", publicationNode);
			throw new RuntimeException("Publication API failed: Missing 'id' in response");
		}
		String id = publicationNode.get("id").asText();

		checkStatus(id, ivTicketResponse);

		byte[] fileData = downloadArtifact.downloadDoc(id, ivTicketResponse);
		JsonNode response = addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otdsTokenResponseDto.getTicket(), fileData,
				"new");
		log.info(response.toPrettyString());
		return response;
	}

	public void checkStatus(String id, IvTicketResponse ivTicketResponse) {
		JsonNode statusNode = publicationStatus.callPublicationStatus(id, ivTicketResponse);
		String status = statusNode.get("status").asText();
		while (status.equals("Complete") || status.equals("Failed") ? false : true) {
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
	}

	@SuppressWarnings("unchecked")
	private String getContentOfLocation(XmlBannersJson xmlBannersJson, String location) {
		if (xmlBannersJson == null || xmlBannersJson.getBanners() == null)
			return "";

		List<XmlBannersJson.Banner> bannerList = xmlBannersJson.getBanners();
		return bannerList.stream().filter(banner -> location.equals(banner.getLocation())).findFirst().map(banner -> {
			Object content = banner.getTextFragment().getContent();
			boolean horizontal = banner.getTextFragment().getHorizontal();
			boolean includesKey = banner.getTextFragment().getIncludesKey();
			if (content instanceof java.util.Map) {
				Map<String, Object> contentMap = (Map<String, Object>) content;
				if (horizontal && includesKey) {
					return getHorizontallySeparatedMapEntry(contentMap);
				} else if (horizontal && !includesKey) {
					return getHorizontallySeparatedMapValues(contentMap);
				} else if (!horizontal && includesKey) {
					return getMapEntryString(contentMap);
				} else if (!horizontal && !includesKey) {
					return getMapValueOfString(contentMap);
				}
			}
			return content != null ? content.toString() : "";
		}).orElse("");
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
