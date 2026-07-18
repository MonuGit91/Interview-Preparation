package com.supai.app.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MergeRequest {

    // This field will be populated from the Frontend,
    // but EXCLUDED when converting to JSON for the API call.
    // This field will be populated from the Frontend,
    // but EXCLUDED when converting to JSON for the API call.
//     @JsonProperty(value = "userName", access = JsonProperty.Access.WRITE_ONLY)
//     private String userName;
	
	@JsonProperty(value = "addingVersion", access = JsonProperty.Access.WRITE_ONLY)
	private boolean addingVersion = false;
	
	@JsonProperty(value = "verificationTimeOut", access = JsonProperty.Access.WRITE_ONLY)
	private int verificationTimeOut = 5;
	
	@JsonProperty(value = "mainDocId", access = JsonProperty.Access.WRITE_ONLY)
	private Long mainDocId;

    private List<NodeItem> nodes;
    private OutputOptions outputOptions;

    @Data
    public static class NodeItem {
        private Long id;
        private Integer order;
    }

    @Data
    public static class OutputOptions {
        private String destinationType;
        private String destinationFilename;
        private Long destinationNode;
        private String format;
    }
}