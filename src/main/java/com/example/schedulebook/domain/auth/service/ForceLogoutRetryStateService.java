package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterAggregateType;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterSource;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterType;
import com.example.schedulebook.domain.deadletter.service.DeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForceLogoutRetryStateService {
    private final ForceLogoutRetryService forceLogoutRetryService;
    private final DeadLetterService deadLetterService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeFailure(ForceLogoutRetry forceLogoutRetry, String reason, String claimToken, Exception e) {
        forceLogoutRetryService.markFailed(forceLogoutRetry.getId(), reason, claimToken);

        try {
            deadLetterService.save(
                    DeadLetterType.FORCE_LOGOUT,
                    DeadLetterSource.FORCE_LOGOUT_RETRY_SCHEDULER,
                    DeadLetterAggregateType.SESSION,
                    forceLogoutRetry.getSessionId(),
                    forceLogoutRetry.getUserId(),
                    forceLogoutRetry.getPayload(),
                    e.getMessage(),
                    e.getClass().getSimpleName(),
                    forceLogoutRetry.getRetryCount() + 1
            );

        } catch (Exception dlqException) {
            log.error("DLQ 저장 실패", dlqException);

            throw new BaseException(ErrorEnum.DEAD_LETTER_SAVE_FAILED, dlqException);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeSuccess(ForceLogoutRetry forceLogoutRetry, String claimToken) {
        forceLogoutRetryService.markSuccess(forceLogoutRetry.getId(), claimToken);
    }
}
