package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.entity.ForceLogoutRetry;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.auth.repository.ForceLogoutRetryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForceLogoutRetryService {
    private final ForceLogoutRetryRepository forceLogoutRetryRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String sessionId, Long userId, ForceLogoutSessionEvent event, String reason) {
        try {
            String json = objectMapper.writeValueAsString(event);

            forceLogoutRetryRepository.save(ForceLogoutRetry.create(
                    sessionId,
                    userId,
                    json,
                    reason
            ));

        }  catch (JsonProcessingException exception) {
            log.error("강제 로그아웃 재시도 payload 직렬화 실패", exception);
            throw new BaseException(ErrorEnum.JSON_SERIALIZATION_FAILED, exception);
        }
    }

    @Transactional
    public void markSuccess(Long forceLogoutRetryId, String claimToken) {
        if (forceLogoutRetryRepository.markSuccess(forceLogoutRetryId, claimToken) != 1) {
            throw new BaseException(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);
        }
    }

    @Transactional
    public void markFailed(Long forceLogoutRetryId, String reason, String claimToken) {
        if (forceLogoutRetryRepository.markFailed(forceLogoutRetryId, reason, claimToken) != 1) {
            throw new BaseException(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String markProcessing(Long forceLogoutRetryId) {
        String claimToken = UUID.randomUUID().toString();

        boolean claimed = forceLogoutRetryRepository.markProcessing(
                forceLogoutRetryId,
                claimToken,
                LocalDateTime.now().minusMinutes(10)
        ) == 1;

        if (claimed) {
            return claimToken;
        }

        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetry(Long forceLogoutRetryId, String reason, int retryCount, String claimToken) {
        LocalDateTime delay = LocalDateTime.now().plusSeconds(nextDelaySeconds(retryCount + 1));

        if (forceLogoutRetryRepository.markRetry(forceLogoutRetryId, reason, delay, claimToken) != 1) {
            throw new BaseException(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<ForceLogoutRetry> findRetryTargets(int size) {
        return forceLogoutRetryRepository.findRetryTargets(
                LocalDateTime.now().minusMinutes(10),
                PageRequest.of(0, size)
        ).getContent();
    }

    @Transactional(readOnly = true)
    public ForceLogoutRetry findById(Long forceLogoutRetryId) {
        return forceLogoutRetryRepository.findById(forceLogoutRetryId).orElseThrow(
                () -> new BaseException(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND)
        );
    }

    @Transactional(readOnly = true)
    public ForceLogoutSessionEvent deserialize(ForceLogoutRetry forceLogoutRetry) {
        try {
            return objectMapper.readValue(
                    forceLogoutRetry.getPayload(),
                    ForceLogoutSessionEvent.class
            );

        } catch (JsonProcessingException e) {
            log.error("강제 로그아웃 payload 역직렬화 실패 forceLogoutRetryId = {}", forceLogoutRetry.getId(), e);

            throw new BaseException(ErrorEnum.JSON_DESERIALIZATION_FAILED, e);
        }
    }

    @Transactional
    public void recover(String aggregateId) {
        ForceLogoutRetry forceLogoutRetry = forceLogoutRetryRepository.findBySessionId(aggregateId).orElseThrow(
                () -> new BaseException(ErrorEnum.FORCE_LOGOUT_RETRY_NOT_FOUND)
        );

        int updated = forceLogoutRetryRepository.updateRecover(forceLogoutRetry.getId(), forceLogoutRetry.getClaimToken());

        if (updated == 0) {
            log.warn("강제 로그아웃 재시도 {} 복구 건너뜀 : 이미 다른 트랜잭션에서 상태 변경됨", forceLogoutRetry.getId());

            throw new BaseException(ErrorEnum.DEAD_LETTER_RECOVER_FAILED);
        } else {
            log.debug("강제 로그아웃 재시도 {} 상태 {}로 복구 성공",
                    forceLogoutRetry.getId(),
                    forceLogoutRetry.getForceLogoutRetryStatus()
            );
        }
    }

    private long nextDelaySeconds(int retryCount) {
        int safeRetry = Math.max(0, Math.min(retryCount, 10));

        return Math.min(CommonConst.NEXT_RETRY_DELAY * (1L << safeRetry), 3600L);
    }
}
