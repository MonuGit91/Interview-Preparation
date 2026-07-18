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
public class GraphQlApiPojo_backup {
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

    @JsonProperty(value = "pageRange", access = JsonProperty.Access.WRITE_ONLY)
    private String pageRange;

    @JsonProperty(value = "includePageNo", access = JsonProperty.Access.WRITE_ONLY)
    private Boolean includePageNo = true;

    private String operationName = null;
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

        // Java API Generated
        private String id = UUID.randomUUID().toString();
        private String pid;
        private String source;
        // Defaults
        private String type = "raster";
        private String version = "1-1-0";
        private List<Object> tags = new ArrayList<>();
        private boolean deleted = false;
        private boolean isDirty = true;
        // For Image
        private String uri = "image uri";
        private String name = "image name";
        private String mimeType = "image/png";
        private List<Double> matrix = Arrays.asList(1.468293778976973, 0.0, 3641.4732736411092, 0.0, 1.4682937789769732,
                285.316551778743);

        // Java Custom Timestamps
        private long modifiedTimeStamp = Instant.now().getEpochSecond();
        private long createdTimeStamp = Instant.now().getEpochSecond();

        // From Frontend (Require setters)
        private String author;
        private int page;
        private int imageWidth;
        private int imageHeight;
        // Nested Objects with Defaults
        private PageExtents pageExtents = new PageExtents();
        private Viewstate viewstate = new Viewstate();
    }

    @Data
    public static class PageExtents {
        // Hardcoded A4 Landscape Constants (Brava Units)
        private double llx = 0;
        private double lly = 0;
        private double urx = 11692.9; // A4 Width
        private double ury = 8268.11; // A4 Height
    }

    @Data
    public static class Viewstate {
        private int page;
        private String scalePreset = "width";
        private double scale = 1.0; // Default zoom level for viewer
        private EyePoint eyePoint = new EyePoint();
        private int rotation = 0;
        private List<Object> hiddenLayers = new ArrayList<>();
    }

    @Data
    public static class EyePoint {
        private double x;
        private double y; 
    }
}