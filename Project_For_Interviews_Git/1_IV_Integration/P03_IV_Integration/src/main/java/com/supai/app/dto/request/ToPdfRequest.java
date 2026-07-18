package com.supai.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToPdfRequest {
//    private String baseUrl;
//    private String userName;
    private String nodeId;
    @Builder.Default
    private Boolean includePageNo = true;
    @Builder.Default
    private String finalDocName = "with_banner";
}