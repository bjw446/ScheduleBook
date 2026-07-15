package com.example.schedulebook.common.exception;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class SessionLimitException extends BaseException {
    private final List<SessionInfoResponse> sessionInfoResponses;

    public SessionLimitException(ErrorEnum errorEnum, List<SessionInfoResponse> sessionInfoResponses) {
        super(errorEnum);
        this.sessionInfoResponses = sessionInfoResponses;
    }
}
