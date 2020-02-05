package com.revolut.client;

import lombok.Data;

@Data
public class AppException extends Exception {

    private int httpCode;

    public AppException() {
    }

    public AppException(String message, int httpCode) {
        super(message);
        this.httpCode = httpCode;
    }

    public AppException(String message, Throwable cause, int httpCode) {
        super(message, cause);
        this.httpCode = httpCode;
    }

    public AppException(Throwable cause, int httpCode) {
        super(cause);
        this.httpCode = httpCode;
    }

    public AppException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int httpCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.httpCode = httpCode;
    }
}
