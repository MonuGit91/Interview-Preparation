package com.supai.app.exception;

public class IllegalClientSecretException extends RuntimeException {

    public IllegalClientSecretException() {
        super("Client secret is invalid or improperly configured.");
    }

    public IllegalClientSecretException(String message) {
        super(message);
    }

    public IllegalClientSecretException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalClientSecretException(Throwable cause) {
        super(cause);
    }
}
