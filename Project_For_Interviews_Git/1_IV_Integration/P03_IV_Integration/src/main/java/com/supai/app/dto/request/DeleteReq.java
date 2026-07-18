package com.supai.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeleteReq {
//	 private String baseUrl;
//	 private String userName;
	private String nodeId;
	private String versionNo;
	private boolean deleteAll;
}
