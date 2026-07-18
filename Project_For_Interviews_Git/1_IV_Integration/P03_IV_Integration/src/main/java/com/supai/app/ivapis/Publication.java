package com.supai.app.ivapis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supai.app.config.BannerConfig;
import com.supai.app.config.IvConfig;
import com.supai.app.constants.Rock;
import com.supai.app.dto.request.StampRequest;
import com.supai.app.dto.request.ToPdfRequest;
import com.supai.app.dto.request.TopStampRequest;
import com.supai.app.exceptions.BannerValidationException;
import com.supai.app.exceptions.ExternalApiException;
import com.supai.app.ivapis.dto.response.IvTicketResponse;
import com.supai.app.services.common.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@Slf4j
@RequiredArgsConstructor
public class Publication {

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final IvConfig ivConfig;
    private final JsonUtil jsonUtil;
    private final BannerConfig bannerConfig;

    private Response publicationApi(String finalJson, String url, String bearerToken) throws IOException {
        log.info("Start : {}", url);
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(finalJson, mediaType);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + bearerToken)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        log.info("End : {}", url);
        return response;
    }

    public String createPdfPublication(String nodeId, IvTicketResponse ivTicketResponse, int maxVersionNo, String url,
            String base64EncodedXML)
            throws IOException {
        // 1. Load and Modify the JSON Template
        File templateFile = new File("publication_template1.json");
        JsonNode root = mapper.readTree(templateFile);

        String pubValue = ivConfig.getBase() != null ? ivConfig.getBase().getPubValue() : "";
        String verId = (ivTicketResponse.getValidNodes() != null && !ivTicketResponse.getValidNodes().isEmpty())
                ? ivTicketResponse.getValidNodes().get(0).getVerId()
                : "";
        String markupToken = ivTicketResponse.getMarkupToken();

        String finalJson = mapper.writeValueAsString(root)
                .replace("{baseUrl}", safeString(pubValue))
                .replace("{nodeId}", safeString(nodeId))
                .replace("{versionNo}", String.valueOf(maxVersionNo))
                .replace("{ver_id}", safeString(verId))
                .replace("{markupToken}", safeString(markupToken))
                .replace("{incodedXML}", safeString(base64EncodedXML));
        log.info("Publication body: {}", finalJson);

        // 3. Execute
        try (Response response = publicationApi(finalJson, url, ivTicketResponse.getToken())) {
            if (!response.isSuccessful()) {
                String errorDetails = response.body() != null ? response.body().string() : "{}";
                JsonNode errorNode;
                try {
                    errorNode = mapper.readTree(errorDetails);
                } catch (Exception e) {
                    errorNode = mapper.createObjectNode().put("message", errorDetails);
                }
                throw new ExternalApiException(response.code(), errorNode, "IV Publication", url);
            }
            return response.body().string();
        }
    }

    public JsonNode customePagePublication(String nodeId, IvTicketResponse ivTicketResponse, int maxVersionNo,
            String url,
            String base64EncodedXML, String pageRange, int totalPages)
            throws IOException {
        String publicationUrl = ivConfig.getApi().getPublication();
        // 1. Load and Modify the JSON Template
        File templateFile = new File("publication_template2.json");
        JsonNode root = mapper.readTree(templateFile);

        String pubValue = ivConfig.getBase().getPubValue();
        String verId = ivTicketResponse.getValidNodes().get(0).getVerId();
        String markupToken = ivTicketResponse.getMarkupToken();
        pageRange = pageRange.toLowerCase().equals("all") ? String.format("%d-%d", 0, totalPages) : pageRange;

        String finalJson = mapper.writeValueAsString(root)
                .replace("{baseUrl}", safeString(pubValue))
                .replace("{nodeId}", safeString(nodeId))
                .replace("{versionNo}", String.valueOf(maxVersionNo))
                .replace("{ver_id}", safeString(verId))
                .replace("{markupToken}", safeString(markupToken))
                .replace("{pagrRange}", pageRange)
                .replace("{incodedXML}", safeString(base64EncodedXML));

        log.info("Publication body: {}", finalJson);

        // 3. Execute
        try (Response response = publicationApi(finalJson, publicationUrl, ivTicketResponse.getToken())) {
            if (!response.isSuccessful()) {
                String errorDetails = response.body() != null ? response.body().string() : "{}";
                JsonNode errorNode;
                try {
                    errorNode = mapper.readTree(errorDetails);
                } catch (Exception e) {
                    errorNode = mapper.createObjectNode().put("message", errorDetails);
                }
                throw new ExternalApiException(response.code(), errorNode, "IV Publication", publicationUrl);
            }
            return mapper.readTree(response.body().string());
        }
    }

    public JsonNode applyQrPublication(String nodeId, IvTicketResponse ivTicketResponse, int maxVersionNo,
            String base64EncodedXML, String pageRange) {
        try {
            String publicationUrl = ivConfig.getApi().getPublication();
            // 1. Load and Modify the JSON Template
            File templateFile = new File("publication_template_applyQRCode.json");
            JsonNode root = mapper.readTree(templateFile);

            if (root.has("featureSettings") && root.get("featureSettings").isArray()) {
                com.fasterxml.jackson.databind.node.ArrayNode featureSettings = (com.fasterxml.jackson.databind.node.ArrayNode) root
                        .get("featureSettings");

                // 1. Add SetExportPageList if needed
                if (pageRange != null && !pageRange.equalsIgnoreCase("all")) {
                    com.fasterxml.jackson.databind.node.ObjectNode exportPageList = mapper.createObjectNode();
                    com.fasterxml.jackson.databind.node.ObjectNode feature = mapper.createObjectNode();
                    feature.put("namespace", "opentext.publishing.content");
                    feature.put("name", "SetExportPageList");
                    feature.put("version", "1.x");
                    exportPageList.set("feature", feature);
                    exportPageList.put("path", "/exportPageList");
                    exportPageList.put("value", "{pagrRange}");
                    featureSettings.add(exportPageList);
                }

                // 2. Add ApplyBannersWatermarks
                com.fasterxml.jackson.databind.node.ObjectNode applyBanners = mapper.createObjectNode();
                com.fasterxml.jackson.databind.node.ObjectNode featureBanners = mapper.createObjectNode();
                featureBanners.put("namespace", "opentext.publishing.content");
                featureBanners.put("name", "ApplyBannersWatermarks");
                featureBanners.put("version", "1.x");
                applyBanners.set("feature", featureBanners);
                applyBanners.put("path", "/url");
                applyBanners.put("value", "data:application/xml;base64,{incodedXML}");
                featureSettings.add(applyBanners);
            }

            String pubValue = ivConfig.getBase() != null ? ivConfig.getBase().getPubValue() : "";
            String verId = (ivTicketResponse.getValidNodes() != null && !ivTicketResponse.getValidNodes().isEmpty())
                    ? ivTicketResponse.getValidNodes().get(0).getVerId()
                    : "";
            String markupToken = ivTicketResponse.getMarkupToken() != null ? ivTicketResponse.getMarkupToken() : "";

            String finalJson = mapper.writeValueAsString(root)
                    .replace("{baseUrl}", safeString(pubValue))
                    .replace("{nodeId}", safeString(nodeId))
                    .replace("{versionNo}", String.valueOf(maxVersionNo))
                    .replace("{ver_id}", safeString(verId))
                    .replace("{markupToken}", safeString(markupToken))
                    .replace("{pagrRange}", safeString(pageRange))
                    .replace("{incodedXML}", safeString(base64EncodedXML));

            log.info("Publication body: {}", finalJson);

            // 3. Execute
            try (Response response = publicationApi(finalJson, publicationUrl, ivTicketResponse.getToken())) {
                if (!response.isSuccessful()) {
                    String errorDetails = response.body() != null ? response.body().string() : "{}";
                    JsonNode errorNode;
                    try {
                        errorNode = mapper.readTree(errorDetails);
                    } catch (Exception e) {
                        errorNode = mapper.createObjectNode().put("message", errorDetails);
                    }
                    throw new ExternalApiException(response.code(), errorNode, "IV Publication", publicationUrl);
                }
                return mapper.readTree(response.body().string());
            }
        } catch (Exception e) {
            log.error(e.toString());
            throw new RuntimeException(e.getMessage());
        }

    }

    public JsonNode applyQRPublication(String nodeId, IvTicketResponse ivTicketResponse, int maxVersionNo,
            String url,
            String base64EncodedXML, String pageRange)
            throws IOException {
        String publicationUrl = ivConfig.getApi().getPublication();
        // 1. Load and Modify the JSON Template
        File templateFile = new File("publication_template2.json");
        JsonNode root = mapper.readTree(templateFile);

        String pubValue = ivConfig.getBase() != null ? ivConfig.getBase().getPubValue() : "";
        String verId = (ivTicketResponse.getValidNodes() != null && !ivTicketResponse.getValidNodes().isEmpty())
                ? ivTicketResponse.getValidNodes().get(0).getVerId()
                : "";
        String markupToken = ivTicketResponse.getMarkupToken() != null ? ivTicketResponse.getMarkupToken() : "";

        String finalJson = mapper.writeValueAsString(root)
                .replace("{baseUrl}", safeString(pubValue))
                .replace("{nodeId}", safeString(nodeId))
                .replace("{versionNo}", String.valueOf(maxVersionNo))
                .replace("{ver_id}", safeString(verId))
                .replace("{markupToken}", safeString(markupToken))
                .replace("{pagrRange}", pageRange)
                .replace("{incodedXML}", safeString(base64EncodedXML));

        log.info("Publication body: {}", finalJson);

        // 3. Execute
        try (Response response = publicationApi(finalJson, publicationUrl, ivTicketResponse.getToken())) {
            if (!response.isSuccessful()) {
                String errorDetails = response.body() != null ? response.body().string() : "{}";
                JsonNode errorNode;
                try {
                    errorNode = mapper.readTree(errorDetails);
                } catch (Exception e) {
                    errorNode = mapper.createObjectNode().put("message", errorDetails);
                }
                throw new ExternalApiException(response.code(), errorNode, "IV Publication", publicationUrl);
            }
            return mapper.readTree(response.body().string());
        }
    }

    private String safeString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public JsonNode callPdfPublicatinApi(String nodeId, IvTicketResponse ivTicketResponse, int maxVersionNo,
            boolean hasBanner,
            ToPdfRequest request) {
        String body = "";
        String url = ivConfig.getApi().getPublication();

        try {
            // // 1. Read XML Template
            String xmlContent = Files.readString(Paths.get("publication_template_1.xml"),
                    java.nio.charset.StandardCharsets.UTF_8);
            xmlContent = applyOrRemoveBanners(xmlContent, request, hasBanner);
            String base64EncodedXML = Base64.getEncoder()
                    .encodeToString(xmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            body = createPdfPublication(nodeId, ivTicketResponse, maxVersionNo, url, base64EncodedXML);
            JsonNode node = mapper.readTree(body);
            return node;
        } catch (IOException e) {
            log.error("Failed callPdfPublicatinApi: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "IV Publication", url);
        }
    }

    public JsonNode callPdfPublicatinApi_(String nodeId, IvTicketResponse ivTicketResponse, int maxVersionNo,
            boolean hasBanner, String base64EncodedXML) {
        String body = "";
        String url = ivConfig.getApi().getPublication();

        try {

            body = createPdfPublication(nodeId, ivTicketResponse, maxVersionNo, url, base64EncodedXML);
            JsonNode node = mapper.readTree(body);
            return node;
        } catch (IOException e) {
            log.error("Failed callPdfPublicatinApi: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "IV Publication", url);
        }
    }

    private String applyOrRemoveBanners(String xmlContent, ToPdfRequest request, boolean hasBanner) {
        // Assuming others might be BLACK, though we clear them.
        String colour = Rock.Blue;
        if (hasBanner) {
            // Pattern matching: checks if request is TopStampRequest and casts it to
            // 'bannerReq'
            if (request instanceof TopStampRequest) {
                TopStampRequest bannerReq = (TopStampRequest) request;
                String banner = bannerReq.getTopBanner();

                if (bannerConfig.getTopBanners().contains(banner)) {

                    xmlContent = xmlContent.replace("{banner_text_TopCenter}", banner);
                    if (!banner.equals("Effective"))
                        colour = Rock.Black;
                    xmlContent = xmlContent.replace("{banner_colour_TopCenter}", colour);
                } else {
                    Map<String, String> map = new HashMap<>();
                    map.put("Error", "Invalid banner");
                    map.put("Available Banners", bannerConfig.getTopBanners().toString());
                    map.put("provided Banner", banner);

                    // Throw custom BannerValidationException (clean JSON, 400 Bad Request, no URL)
                    throw new BannerValidationException(jsonUtil.mapToJsonNode(map));
                }
            } else {
                // Should not happen if logic is correct, but safe fallback
                xmlContent = xmlContent.replace("{banner_text_TopCenter}", "");
                xmlContent = xmlContent.replace("{banner_colour_TopCenter}", Rock.Blue);
            }
        } else {
            xmlContent = xmlContent.replace("{banner_text_TopCenter}", "");
            xmlContent = xmlContent.replace("{banner_colour_TopCenter}", Rock.Blue);
        }

        // Clear other banners as requested ("only top banner should be apply")
        xmlContent = xmlContent.replace("{banner_text_BottomLeft}", "");
        xmlContent = xmlContent.replace("{banner_text_BottomCenter}", "");
        xmlContent = xmlContent.replace("{banner_text_BottomRight}", "");

        // Set their colors to default (or whatever, text is empty anyway)
        xmlContent = xmlContent.replace("{banner_colour_BottomLeft}", Rock.Black);
        xmlContent = xmlContent.replace("{banner_colour_BottomCenter}", Rock.Black);
        xmlContent = xmlContent.replace("{banner_colour_BottomRight}", Rock.Black);

        return xmlContent; // No longer replacing {topBanner} as we use template 1 now
    }

    public JsonNode callStampedPublicationApi(String nodeId, IvTicketResponse ivTicketResponse, int maxVersionNo,
            StampRequest request) {
        String body = "";
        String url = ivConfig.getApi().getPublication();

        try {
            // 1. Read XML Template
            String xmlContent = Files.readString(Paths.get("publication_template_1.xml"),
                    java.nio.charset.StandardCharsets.UTF_8);

            // 2. Adjust values based on StampRequest
            xmlContent = applyStampBanners(xmlContent, request);

            // 3. Base64 Encode
            String base64EncodedXML = Base64.getEncoder()
                    .encodeToString(xmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            body = createPdfPublication(nodeId, ivTicketResponse, maxVersionNo, url, base64EncodedXML);
            return mapper.readTree(body);
        } catch (IOException e) {
            log.error("Failed callStampedPublicationApi: {}", e.getMessage());
            JsonNode errorNode = jsonUtil.getJson("Error", e.getMessage());
            throw new ExternalApiException(502, errorNode, "IV Publication (Stamp)", url);
        }
    }

    private String applyStampBanners(String xmlContent, StampRequest request) {

        // Replace Texts
        xmlContent = xmlContent.replace("{banner_text_TopCenter}", "");

        String footerLeftText = String.format(
                "Document No.: %s | Revision No.: %02d | Copy No.: %d | Printed By: %s | Printed On: %s",
                request.getDocNumber(),
                request.getRevisionNo(),
                request.getCopyNo(),
                request.getPrintedBy(),
                request.getPrintedOn());

        xmlContent = xmlContent.replace("{banner_text_BottomLeft}", footerLeftText);
        xmlContent = xmlContent.replace("{banner_text_BottomCenter}", "");
        xmlContent = xmlContent.replace("{banner_text_BottomRight}", "");

        // Replace Colors
        xmlContent = xmlContent.replace("{banner_colour_TopCenter}", Rock.Blue);
        xmlContent = xmlContent.replace("{banner_colour_BottomLeft}", Rock.Blue);
        xmlContent = xmlContent.replace("{banner_colour_BottomCenter}", Rock.Blue);
        xmlContent = xmlContent.replace("{banner_colour_BottomRight}", Rock.Blue);

        return xmlContent;
    }
}