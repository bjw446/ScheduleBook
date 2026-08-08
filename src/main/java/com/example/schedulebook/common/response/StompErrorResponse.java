package com.example.schedulebook.common.response;

public record StompErrorResponse(
        String code,
        int status,
        String message
) {
}