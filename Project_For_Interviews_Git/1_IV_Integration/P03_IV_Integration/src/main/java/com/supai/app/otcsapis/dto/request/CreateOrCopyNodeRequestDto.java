package com.supai.app.otcsapis.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrCopyNodeRequestDto {
	private Long parent_id;
	private Long original_id;
	private String name;
}
