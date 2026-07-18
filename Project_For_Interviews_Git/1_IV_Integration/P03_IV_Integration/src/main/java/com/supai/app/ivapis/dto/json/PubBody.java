package com.supai.app.ivapis.dto.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// {"nodes":[{"id":17973691,"vernum":1,"vertype":""}]}
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PubBody {
    List<NodesItems> nodes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodesItems {
    	@JsonProperty("id")
        private Long nodeId;
        
//        @JsonProperty("vernum")
//        private Integer versionNo;
//
//        @JsonProperty("vertype")
//        private String versionType;
    }

    public static PubBody getBody(Long nodeId, Integer versionNo) {
        return PubBody.builder()
                .nodes(List.of(NodesItems.builder()
                        .nodeId(nodeId)
//                        .versionNo(versionNo)
//                        .versionType("")
                        .build()))
                .build();
    }
}
