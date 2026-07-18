package com.supai.app.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.IvConfig;
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtdsConfig;
import com.supai.app.dto.request.GraphQlApiPojo;
import com.supai.app.dto.request.MergeRequest;
import com.supai.app.dto.request.StampRequest;
import com.supai.app.dto.request.ToPdfRequest;
import com.supai.app.dto.request.TopStampRequest;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.ivapis.ApplyQr;
import com.supai.app.ivapis.DownloadArtifact;
import com.supai.app.ivapis.IVTicket;
import com.supai.app.ivapis.Merge;
import com.supai.app.ivapis.Publication;
import com.supai.app.ivapis.PublicationDetails;
import com.supai.app.ivapis.PublicationStatus;
import com.supai.app.ivapis.dto.response.IvTicketResponse;
import com.supai.app.otcsapis.AddVersion;
import com.supai.app.otcsapis.AllVersions;
import com.supai.app.otcsapis.OtcsToken;
import com.supai.app.otcsapis.SubNodeProperty;
import com.supai.app.otcsapis.dto.response.ChildNodePropertyResponseDto;
import com.supai.app.otcsapis.dto.response.OtdsTokenResponseDto;
import com.supai.app.otds.OtdsToken;
import com.supai.app.services.common.JsonUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToPdfSercice {
	private final IVTicket iVTicket;
	private final Publication publication;
	private final PublicationStatus publicationStatus;
	private final DownloadArtifact downloadArtifact;
	private final AddVersion addVersion;
	private final AllVersions allVersions;
//	private final ImpersonateUser impersonateUser;
	private final Merge merge;
	private final JsonUtil jsonUtil;
	private final ApplyQr applyQr;
	private final PdfService1 pdfService1;
	private final PublicationDetails publicationDetails;
	private final OtdsConfig otdsConfig;
	private final OtcsApi otcsApi;
//	private final UploadToCS uploadToCS;
	private final OtcsToken otcsToken;
	private final OtdsToken otdsToken;
	private final SubNodeProperty subNodeProperty;
	private final IvConfig ivConfig;
	@Data
	@AllArgsConstructor
	private class MergeDocVerification {
		private boolean docNamePresent;
		private ChildNodePropertyResponseDto.Results.Data.Properties docProperties;
		private ChildNodePropertyResponseDto cheildNodes;
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

	public JsonNode pdfAgent(ToPdfRequest request) {

//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);
		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTicket,
				request.getNodeId(), maxVersionNo);

		// String bearerToken = itcketNode.path("token").asText();

		JsonNode publicationNode = publication.callPdfPublicatinApi(request.getNodeId(), ivTicketResponse, maxVersionNo,
				false, request);
		String id = publicationNode.get("id").asText();

		checkStatus(id, ivTicketResponse);

		byte[] fileData = downloadArtifact.downloadDoc(id, ivTicketResponse);
		JsonNode response = addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otdsTicket, fileData,
				"new");
		log.info(response.toPrettyString());
		return response;
	}

	public JsonNode pdfBannerAgent(TopStampRequest request) {

//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);

		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTicket,
				request.getNodeId(), maxVersionNo);

//		String bearerToken = ivTicketResponse.getToken();

		JsonNode publicationNode = publication.callPdfPublicatinApi(request.getNodeId(), ivTicketResponse, maxVersionNo,
				true, request);
		String id = publicationNode.get("id").asText();
		checkStatus(id, ivTicketResponse);
		byte[] fileData = downloadArtifact.downloadDoc(id, ivTicketResponse);
		JsonNode response = addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otdsTicket, fileData,
				"new");
		log.info(response.toPrettyString());
		return response;
	}

	public JsonNode pdfStampAgent(StampRequest request) {

//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);

		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTicket,
				request.getNodeId(), maxVersionNo);

		// String bearerToken = ivTicketResponse.getToken();

		JsonNode publicationNode = publication.callStampedPublicationApi(request.getNodeId(), ivTicketResponse,
				maxVersionNo, request);
		String id = publicationNode.get("id").asText();
		checkStatus(id, ivTicketResponse);
		byte[] fileData = downloadArtifact.downloadDoc(id, ivTicketResponse);
		JsonNode response = addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otdsTicket, fileData,
				"new");
		log.info(response.toPrettyString());
		return response;
	}

	public JsonNode mergePdfAgent(MergeRequest request) {
		String jsonString = jsonUtil.classToJson(request);
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
//		int maxVersionNo = allVersions.getMaxVersion("" + request.getMainDocId(), otcsTicket);
//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		long destinationDocId = 123L;

		ObjectNode response = (ObjectNode) merge.transformNodes(otdsTicket, jsonString);
		
		MergeDocVerification mergeDocVerification = isNamePresentInDestnitation(request.getOutputOptions().getDestinationNode(), request.getOutputOptions().getDestinationFilename(), otcsTicket);

		if (mergeDocVerification.isDocNamePresent()) {
			log.warn("Api will add version because - name [{}] is present in [{}]",
					request.getOutputOptions().getDestinationFilename(),
					request.getOutputOptions().getDestinationNode());
			
			ChildNodePropertyResponseDto.Results.Data.Properties destinationDocProperty = mergeDocVerification.getDocProperties();
			destinationDocId = destinationDocProperty.getId();
			int destinationDocMaxVersion = destinationDocProperty.getWnd_version();
			
			Instant dedline = Instant.now().plus(Duration.ofMinutes(request.getVerificationTimeOut()));
			boolean foundNewVersion = false;
			while (Instant.now().isBefore(dedline)) { // check
				try {
					Thread.sleep(2 * 1000); // 2 sec
				} catch (InterruptedException e) {
					
				}
				otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
				JsonNode nodeVersionResponse = (ObjectNode) allVersions.getNodeVersions("" + destinationDocId, otcsTicket);
				int destinationDocNewMaxVersion = allVersions.extractMaxVersion(nodeVersionResponse);

				log.info("{} - Initial Max Version : {} , Current Max Version : {}",destinationDocId, destinationDocMaxVersion, destinationDocNewMaxVersion);
				if (destinationDocMaxVersion < destinationDocNewMaxVersion) {
					foundNewVersion = true;
					break;
				}
			}
			if (!foundNewVersion) {
				response.put("success", false);
				response.put("error", "New version not found.");
				throw new ExternalApiException(500, response, "Merge Document", ivConfig.getApi().getMerge());
			}

		} else {
			log.warn("Api will create object because - name [{}] is not present in [{}]",
					request.getOutputOptions().getDestinationFilename(),
					request.getOutputOptions().getDestinationNode());

			Instant dedline = Instant.now().plus(Duration.ofMinutes(request.getVerificationTimeOut()));
			
			boolean isDocCreated = false;
			while(Instant.now().isBefore(dedline)) {
				try {
					Thread.sleep(2 * 1000); // 2 sec
				} catch (InterruptedException e) {

				}
				otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
				MergeDocVerification docVerification = isNamePresentInDestnitation(request.getOutputOptions().getDestinationNode(), request.getOutputOptions().getDestinationFilename(), otcsTicket);
				if(docVerification.isDocNamePresent()) {
					isDocCreated = true;
					destinationDocId = docVerification.getDocProperties().getId();
					break;
				}
			}
			if(!isDocCreated) {
				response.put("success", false);
				response.put("error", "Name [{name}] not found in [{parentId}]."
						.replace("{name}", request.getOutputOptions().getDestinationFilename())
						.replace("{parentId}", ""+request.getOutputOptions().getDestinationNode()));
				throw new ExternalApiException(500, response, "Merge Document", ivConfig.getApi().getMerge());
			}
		}
		response.put("success", true);
		response.put("destinationNodeId", destinationDocId);
		log.info("Response: {}", response.toString());
		return response;
	}

	private MergeDocVerification isNamePresentInDestnitation(long destinationNode, String fileName, String otcsTicket ){
		ChildNodePropertyResponseDto childProperties = subNodeProperty
				.getAllSubNodeProperty(destinationNode, otcsTicket);
		JsonNode propertiesNode = jsonUtil.objectMapper.valueToTree(childProperties);
		log.info("{} - Child Properties: {}", destinationNode, propertiesNode.toString());
		
		boolean isNamePresent[] = { false };
		ChildNodePropertyResponseDto.Results.Data.Properties nodeProperties[] = {null};
		childProperties.getResults().forEach(result -> {
			String name = result.getData().getProperties().getName();
			if (name.equals(fileName)) {
				nodeProperties[0] = result.getData().getProperties();
				isNamePresent[0] = true;
			}
		});
		MergeDocVerification mergeDocVerification = new MergeDocVerification(isNamePresent[0], nodeProperties[0], childProperties);
		return mergeDocVerification;
	}
	
	public JsonNode applyQr(GraphQlApiPojo request) {

		JsonNode otcsTicketResponse = otcsToken.getOtcsTicketJson();
		String otcsTicket = otcsTicketResponse.at("/ticket").asText();

		// 1. Get identifiers from the request
//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);

		// 2. Fetch required IDs from the external API
		IvTicketResponse ivTicketResponse = iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTicket,
				request.getNodeId(), maxVersionNo);

		JsonNode pageDetails = checkPageStatus(ivTicketResponse);

		int pageCount = pageDetails
				.at("/_embedded/pa:get_publication_artifacts/0/_embedded/ac:get_artifact_content/content/pageCount")
				.asInt();
		log.info("Page Count: {}", pageCount);
		// 3. Update the POJO with the fetched IDs

		setGraphQlJson(request, ivTicketResponse, pageCount);

		// 4. GENERATE THE JSON STRING HERE (after all values are set)
		String jsonString = jsonUtil.classToJson(request);
		String token = ivTicketResponse.getToken();

		// 5. Call the service with the correct argument order (Body first, then Ticket)
		JsonNode applyQrGraphQlResponse = applyQr.applyQrCode(jsonString, token);
		log.info("Apply QR GraphQL Response: {}", applyQrGraphQlResponse);
		JsonNode graphQlFollowupResponse = applyQr.applyQrCodeFollowUp(otdsTicket, request.getNodeId(), maxVersionNo);
		log.info("GraphQL Followup Response: {}", graphQlFollowupResponse);

		JsonNode attachQrPublicationStatusResponse = pdfService1.qrAttachmentService(request);
		// return attachQrPublicationStatusResponse;
		byte[] fileData = downloadArtifact.downloadDoc(attachQrPublicationStatusResponse.get("id").asText(),
				ivTicketResponse);

//		JsonNode uploadResponse = uploadToCS.uploadToCS(request.getParentNodeId(), otcsTicketResponse.at("/ticket").asText(), fileData, request.getNewDocName());

//		addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otcsTicket, fileData, "new");
//		JsonNode uploadResponse = uploadToCS.uploadToCS(url, otcsTicket, fileData, fileName, nodeId)
//		log.info(response.toPrettyString());
//		return uploadResponse;
		JsonNode response = addVersion.addVersion(otcsApi.base.getCommon(), request.getNodeId(), otdsTicket, fileData,
				request.getFinalDocName());
		log.info(response.toPrettyString());
		return response;
	}

	private void setGraphQlJson(GraphQlApiPojo request, IvTicketResponse ivTicketResponse, int pageCount) {
		// TODO Auto-generated method stub
		List<GraphQlApiPojo.Markup> markups = request.getVariables().getMarkups();
		if (!markups.isEmpty()) {
			GraphQlApiPojo.Markup baseMarkup = markups.get(0);
			List<GraphQlApiPojo.Markup> clonedMarkups = new ArrayList<>();

			String userId = otdsConfig.getUserId();
			String imgUri = otcsApi.getQrCode().replace("{imgNodeId}", request.getImgNodeId()).replace("{imgVersion}",
					request.getImgVersion());
			String imgName = request.getImgNodeId();
			String pubId = ivTicketResponse.getValidNodes().get(0).getPubId();
			String verId = ivTicketResponse.getValidNodes().get(0).getVerId();

			for (int i = 0; i < pageCount; i++) {
				GraphQlApiPojo.Markup markup = new GraphQlApiPojo.Markup();
				// Copy essential fields
				markup.setMimeType(baseMarkup.getMimeType());
				markup.setMatrix(new ArrayList<>(baseMarkup.getMatrix()));
				markup.setImageWidth(baseMarkup.getImageWidth());
				markup.setImageHeight(baseMarkup.getImageHeight());
//				markup.setPageExtents(baseMarkup.getPageExtents());
				markup.setTitle(baseMarkup.getTitle());

				// Set dynamic values
				markup.setAuthor(userId);
				markup.setUri(imgUri);
				markup.setName(imgName);
				markup.setPid(pubId);
				markup.setSource(verId);
				markup.setPage(i);

				// Update viewstate
//				if (baseMarkup.getViewstate() != null) {
//					GraphQlApiPojo.Viewstate vs = new GraphQlApiPojo.Viewstate();
//					vs.setScale(baseMarkup.getViewstate().getScale());
//					vs.setScalePreset(baseMarkup.getViewstate().getScalePreset());
//					vs.setRotation(baseMarkup.getViewstate().getRotation());
//					vs.setEyePoint(baseMarkup.getViewstate().getEyePoint());
//					vs.setPage(i);
//					markup.setViewstate(vs);
//				}
				clonedMarkups.add(markup);
				if (request.isQrOnFirstPageOnly())
					break;
			}
			request.getVariables().setMarkups(clonedMarkups);
		}
	}

	public IvTicketResponse getBearertoken(ToPdfRequest request) {
		String userName = otdsConfig.getUserId();
//		String otdsTicket = impersonateUser.getOtdsTicket();
		OtdsTokenResponseDto otdsTokenResponseDto = otdsToken.getOtdsToken();
		String otdsTicket = otdsTokenResponseDto.getTicket();
		String otcsTicket = otcsToken.getOtcsTicketJson().at("/ticket").asText();
		int maxVersionNo = allVersions.getMaxVersion(request.getNodeId(), otcsTicket);

		// 2. Fetch required IDs from the external API
		return iVTicket.callPubApi(otcsApi.base.getCommon(), otdsTicket, request.getNodeId(), maxVersionNo);

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

}
