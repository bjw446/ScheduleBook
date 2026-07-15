package com.example.schedulebook.domain.auth.dto.response;

import java.util.List;

public record SessionLimitResponse(
        List<SessionInfoResponse> sessionInfoResponses
) {
}
