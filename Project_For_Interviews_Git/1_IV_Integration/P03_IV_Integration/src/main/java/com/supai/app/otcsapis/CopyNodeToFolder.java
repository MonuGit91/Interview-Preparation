package com.supai.app.otcsapis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.OtcsApi;
import com.supai.app.constants.Rock;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Component
@RequiredArgsConstructor
@Slf4j
public class CopyNodeToFolder {
	private final OkHttpClient okHttpClient;
	private final JsonUtil jsonUtil;
	private final OtcsApi otcsApi;

	private Response uploadDoc(String url, String body, String otcsTicket)
			throws IOException {

		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("body", body)
				.build();

		log.info("Start : {}", url);
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.addHeader(Rock.OtcsTicketKey, otcsTicket)
				.build();

		log.info("Start : {}", url);
		Response response = okHttpClient.newCall(request).execute();
		log.info("End : {}", url);
		return response;
	}

	public JsonNode CopyNodeToParent(String docNodeId, String folderNodeId, String otcsTicket) {
		String url = otcsApi.base.getCommon() + otcsApi.api.getCreateOrCopyNode();
		
		//{"original_id":12345, "parent_id":5678}
		Map<String, String> map = new HashMap<>();
		map.put("original_id", folderNodeId);
		map.put("parent_id", folderNodeId);
		String body = new ObjectMapper().valueToTree(map).asText();
		
		try(Response response = uploadDoc(url, body, otcsTicket)) {
			String responseBody = response.body().string();
			return jsonUtil.getJson(responseBody);
		} catch (IOException apiExc) {
			log.error("Failed to Copy {} To {}", docNodeId, folderNodeId);
			JsonNode errorNode = jsonUtil.getJson("Error", apiExc.getMessage());
			throw new ExternalApiException(500, errorNode, "OTCS Auth", url);	
		}
	}
}