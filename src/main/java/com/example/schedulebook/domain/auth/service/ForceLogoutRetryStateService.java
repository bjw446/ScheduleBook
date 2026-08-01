package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForceLogoutRetryStateService {
    private final ForceLogoutRetryService forceLogoutRetryService;

    @Transactional
    public void completeFailure(ForceLogoutRetry forceLogoutRetry, String reason, String claimToken) {
        forceLogoutRetryService.markFailed(forceLogoutRetry.getId(), reason, claimToken);
    }

    @Transactional
    public void completeSuccess(ForceLogoutRetry forceLogoutRetry, String claimToken) {
        forceLogoutRetryService.markSuccess(forceLogoutRetry.getId(), claimToken);
    }
}
