package com.supai.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomePageRequest {
//    private String baseUrl;
//    private String userName;
    private String nodeId;
    private String pageRange;
    @Builder.Default
    private Boolean includePageNo = false;
}