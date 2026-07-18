package com.supai.app.exception;

import com.supai.app.services.common.JsonObj;

public class PermissionException extends RuntimeException {
	public PermissionException() {
		// do not modify "do not have permission." in message because in frontend based on this
		// showing modal
		super(JsonObj.getJson("error", String.format("do not have permission to perform this acction.")).toString());
	}
}
