package com.supai.app.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StampRequest {
//     @NotBlank(message = "userName is required")
//     private String userName;
//
//     @NotBlank(message = "baseUrl is required")
//     private String baseUrl;

    @NotBlank(message = "nodeId is required")
    private String nodeId;

    @NotBlank(message = "docNumber is required")
    private String docNumber;

    @NotNull(message = "revisionNo is required")
    private Integer revisionNo;

    @NotNull(message = "copyNo is required")
    private Integer copyNo;

    @NotBlank(message = "printedBy is required")
    private String printedBy;

    @NotBlank(message = "printedOn is required")
    private String printedOn;
}
