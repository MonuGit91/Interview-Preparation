package com.supai.app.dto.request;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.supai.app.config.OtdsConfig;

import lombok.Data;

@Data
public class GraphQlApiPojo {
	@JsonProperty(value = "finalDocName", access = JsonProperty.Access.WRITE_ONLY)
    String finalDocName = "with_qr";
	
    @JsonProperty(value = "imgNodeId", access = JsonProperty.Access.WRITE_ONLY)
    String imgNodeId;

    @JsonProperty(value = "imgVersion", access = JsonProperty.Access.WRITE_ONLY)
    String imgVersion;

    @JsonProperty(value = "imgName", access = JsonProperty.Access.WRITE_ONLY)
    String imgName;

    @JsonProperty(value = "nodeId", access = JsonProperty.Access.WRITE_ONLY)
    private String nodeId;

    @JsonProperty(value = "newDocName", access = JsonProperty.Access.WRITE_ONLY)
    private String newDocName;

    @JsonProperty(value = "parentNodeId", access = JsonProperty.Access.WRITE_ONLY)
    private String parentNodeId;
    
    @JsonProperty(value = "qrOnFirstPageOnly", access = JsonProperty.Access.WRITE_ONLY)
    private boolean qrOnFirstPageOnly = false;
    
    @JsonProperty(value = "pageRange", access = JsonProperty.Access.WRITE_ONLY)
    private String pageRange = "all";

    @JsonProperty(value = "includePageNo", access = JsonProperty.Access.WRITE_ONLY)
    private Boolean includePageNo = true;

    private String operationName = null;
    // Standard mutation for saving markups
    private String query = "mutation ($markups: [JSON!]!) { saveMarkups(input: { markups: $markups }) { saved { id } errors { markup { id } error } } }";
    private Variables variables = new Variables();

    @Data
    public static class Variables {
        private List<Markup> markups = new ArrayList<>();
    }

    @Data
    public static class Markup {
        @JsonProperty(access = Access.WRITE_ONLY)
        private OtdsConfig otdsConfig;

        // --- Will be steed ---
        private String id = UUID.randomUUID().toString();
        private String pid; // Usually the NodeID of the document
        private String source; // Application Name
        private String author;

        private String title = "Issuance";

        // --- Defaults ---
        private String type = "raster";
        private String version = "1-1-0";
        private List<Object> tags = new ArrayList<>();
        private boolean deleted = false;
        private boolean isDirty = true;
        private int page = 0;

        // --- Image Metadata (Required from Frontend/Upload) ---
        private String uri = "image uri";
        private String name = "image name";
        private String mimeType = "image/png";

        // --- Dimensions (Required for Rectangle Calculation) ---
        private int imageWidth = 134;
        private int imageHeight = 66;

        // --- Positioning (Calculated via method below) ---
        private List<Double> matrix = Arrays.asList(10.0, 0.0, 5360.0, 0.0, 10.0, 642.0);

        // --- Nested Objects ---
        private PageExtents pageExtents = new PageExtents(); // Defaults to A4
        // private Viewstate viewstate = new Viewstate();

        // --- Timestamps ---
        private long modifiedTimeStamp = Instant.now().getEpochSecond();
        private long createdTimeStamp = Instant.now().getEpochSecond();
    }

    @Data
    public static class PageExtents {
        // Hardcoded A4 Landscape Constants (Brava Units)
        private double llx = 0;
        private double lly = 0;
        private double urx = 8268.11;
        private double ury = 11692.9;
    }
}