package com.filemanager.dto;

public record ApiErrorResponse(
        String message,
        String code,
        String field
) {
    public ApiErrorResponse(String message) {
        this(message, null, null);
    }

    public ApiErrorResponse(String message, String code) {
        this(message, code, null);
    }
}
