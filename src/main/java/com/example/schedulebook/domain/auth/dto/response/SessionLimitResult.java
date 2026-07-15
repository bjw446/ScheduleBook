package com.example.schedulebook.domain.auth.dto.response;

import java.util.List;

public record SessionLimitResult(
        boolean exceeded,
        List<SessionInfoResponse> sessionInfoResponses
) {
    public static SessionLimitResult available() {
        return new SessionLimitResult(false, List.of());
    }

    public static SessionLimitResult exceeded(List<SessionInfoResponse> sessionInfoResponses) {
        return new SessionLimitResult(true, sessionInfoResponses);
    }
}
