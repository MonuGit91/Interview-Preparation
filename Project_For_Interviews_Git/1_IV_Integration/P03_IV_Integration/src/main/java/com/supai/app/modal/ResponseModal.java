package com.supai.app.modal;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public class ResponseModal <T>{
	private boolean isOk;
	ResponseEntity<T> response;
}
