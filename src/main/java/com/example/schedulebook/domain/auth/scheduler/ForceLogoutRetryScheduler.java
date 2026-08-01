package com.example.schedulebook.domain.auth.scheduler;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.dispatcher.ForceLogoutDispatcher;
import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryService;
import com.example.schedulebook.domain.auth.service.ForceLogoutRetryStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForceLogoutRetryScheduler {
    private final ForceLogoutRetryService forceLogoutRetryService;
    private final ForceLogoutRetryStateService forceLogoutRetryStateService;
    private final ForceLogoutDispatcher forceLogoutDispatcher;

    @Scheduled(fixedDelay = 30_000)
    public void process() {
        for (int batch = 0; batch < CommonConst.MAX_BATCHES_PER_RUN; batch++) {
            List<ForceLogoutRetry> forceLogoutRetries = forceLogoutRetryService.findRetryTargets(CommonConst.BATCH_SIZE);

            if (forceLogoutRetries.isEmpty()) {
                break;
            }

            for (ForceLogoutRetry forceLogoutRetry : forceLogoutRetries) {
                String claimToken = forceLogoutRetryService.markProcessing(forceLogoutRetry.getId());

                if (claimToken == null) {
                    continue;
                }

                try {
                    forceLogoutDispatcher.dispatch(forceLogoutRetryService.deserialize(forceLogoutRetry));

                } catch (BaseException e) {
                    if (e.getErrorEnum() == ErrorEnum.JSON_DESERIALIZATION_FAILED) {
                        forceLogoutRetryStateService.completeFailure(forceLogoutRetry, e.getMessage(), claimToken);

                        continue;
                    }

                    retry(forceLogoutRetry, claimToken, e);

                    continue;

                } catch (Exception e) {
                    log.warn("강제 로그아웃 재시도 처리 실패 forceLogoutRetryId = {}", forceLogoutRetry.getId(), e);

                    retry(forceLogoutRetry, claimToken, e);

                    continue;
                }

                try {
                    forceLogoutRetryStateService.completeSuccess(forceLogoutRetry, claimToken);

                } catch (Exception e) {
                    log.error("강제 로그아웃 재시도 완료 상태 갱신 실패 forceLogoutRetryId = {}", forceLogoutRetry.getId(), e);
                }
            }
        }
    }

    private void retry(ForceLogoutRetry forceLogoutRetry, String claimToken, Exception e) {
        try {
            if ((forceLogoutRetry.getRetryCount() + 1) >= CommonConst.MAX_RETRY) {
                forceLogoutRetryStateService.completeFailure(forceLogoutRetry, e.getMessage(), claimToken);

            } else {
                forceLogoutRetryService.markRetry(
                        forceLogoutRetry.getId(),
                        e.getMessage(),
                        forceLogoutRetry.getRetryCount(),
                        claimToken
                );
            }

        } catch (Exception exception) {
            log.error("강제 로그아웃 재시도 상태 갱신 실패 forceLogoutRetryId = {}", forceLogoutRetry.getId(), exception);
        }
    }
}
