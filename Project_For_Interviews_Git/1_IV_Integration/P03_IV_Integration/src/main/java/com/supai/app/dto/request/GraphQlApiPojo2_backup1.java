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
public class GraphQlApiPojo2_backup1 {
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
        private String pid;    // Usually the NodeID of the document
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
        private List<Double> matrix; // Logic handles this now
        
        // --- Nested Objects ---
        private PageExtents pageExtents = new PageExtents(); // Defaults to A4
//        private Viewstate viewstate = new Viewstate();

        // --- Timestamps ---
        private long modifiedTimeStamp = Instant.now().getEpochSecond();
        private long createdTimeStamp = Instant.now().getEpochSecond();

//     // CONSTRUCTOR: This sets your "Calculated Defaults" immediately
//        public Markup() {
//            // Default: Scale 1.0, positioned at  X=100, Y=100
//            this.calculatePlacement(100.0, 100.0, 1.0);
//        }
//        
//        /**
//         * KEY LOGIC: Calculates Matrix and EyePoint based on simple inputs.
//         * Call this method after setting imageWidth and imageHeight.
//         * * @param pageNum The page number (e.g., 1)
//         * @param x       The Left position (Top-Left corner)
//         * @param y       The Top position (Top-Left corner)
//         * @param scale   The Size multiplier (1.0 = Original Size)
//         */
//        public void calculatePlacement(double x, double y, double scale) {
//        	//     matrix: [{Scale X}, {Shear Y (Always 0)}, {X (Top-Left corner)}, {X (Always 0)}, {Scale Y}, {Y (Top-Left corner)}]
//            //ie,  Matrix: [ScaleX, ShearY, X, ShearX, ScaleY, TransY]
//            // We use the same 'scale' for X and Y to maintain the Aspect Ratio (Rectangle shape)
//        	this.matrix = Arrays.asList(scale, 0.0, x, 0.0, scale, y);
//
//            // 3. Calculate EyePoint (Center of the Rectangle) for correct Viewer Focus
//            // Center = Position + (Dimension * Scale / 2)
//            double centerX = x + (this.imageWidth * scale / 2.0);
//            double centerY = y + (this.imageHeight * scale / 2.0);
//            
//            this.viewstate.getEyePoint().setX(centerX);
//            this.viewstate.getEyePoint().setY(centerY);
//        }
    }

    @Data
    public static class PageExtents {
        // Hardcoded A4 Landscape Constants (Brava Units)
        private double llx = 0;
        private double lly = 0;
        private double urx = 8268.11; // A4 Width
        private double ury = 11692.9; // A4 Height
    }

//    @Data
//    public static class Viewstate {
//        private int page = 0;
//        private String scalePreset = "width";
//        private double scale = 1.0; // Default zoom
//        private EyePoint eyePoint = new EyePoint();
//        private int rotation = 0;
//        private List<Object> hiddenLayers = new ArrayList<>();
//    }

//    @Data
//    public static class EyePoint {
//        private double x;
//        private double y; 
//    }
}