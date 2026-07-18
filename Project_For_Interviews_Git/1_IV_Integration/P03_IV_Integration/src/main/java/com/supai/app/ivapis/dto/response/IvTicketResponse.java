package com.supai.app.ivapis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor // Replaces RequiredArgsConstructor for better Jackson compatibility
@AllArgsConstructor
public class IvTicketResponse {
    
    private String initMsg;
    private List<Object> invalidNodes;
    private LoaderOptions loaderOptions;
    private String markupToken;
    private String pubTemplate;
    private String token;
    private List<ValidNodes> validNodes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoaderOptions {
        private Boolean enableNewRendition;
        private String persona;
        private Boolean viewOnly;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidNodes {
        // Changed from List to Map because JSON is "{}" (Object), not "[]" (Array)
        private Map<String, Object> banner;

        @JsonProperty("banner_from_id")
        private Integer bannerFromId;

        private Long id;

        @JsonProperty("pub_id")
        private String pubId;

        @JsonProperty("rendition_type")
        private String renditionType;

        private String title;

        @JsonProperty("ver_id")
        private String verId;

        @JsonProperty("version_number")
        private Integer versionNumber;

        @JsonProperty("wait_for_pubs")
        private List<Object> waitForPubs;
    }
}