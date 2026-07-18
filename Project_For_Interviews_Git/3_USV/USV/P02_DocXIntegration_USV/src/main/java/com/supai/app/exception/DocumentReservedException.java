package com.supai.app.exception;

import com.supai.app.services.common.JsonObj;

public class DocumentReservedException extends RuntimeException {

	public DocumentReservedException() {
		super("Client secret is invalid or improperly configured.");
	}

	public DocumentReservedException(String documentName) {
		// do not modify "is reserved." in message because in frontend based on this
		// showing modal
		super(JsonObj.getJson("error", String.format("The item %s is reserved.", documentName)).toString());
	}
}
