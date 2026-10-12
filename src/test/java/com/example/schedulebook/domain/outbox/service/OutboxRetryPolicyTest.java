package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.outbox.dto.RetryDecision;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRetryPolicyTest {

    private static final long MAX_DELAY_SECONDS = 3600L;

    private final OutboxRetryPolicy outboxRetryPolicy = new OutboxRetryPolicy();

    @Test
    @DisplayName("재시도 횟수가 0이면 FAILED 상태와 기본 대기 시간을 반환한다")
    void decide_재시도_횟수가_0이면_FAILED와_기본_대기_시간을_반환한다() {
        // given
        int retryCount = 0;
        LocalDateTime before = LocalDateTime.now();

        // when
        RetryDecision decision = outboxRetryPolicy.decide(retryCount);

        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(decision.outboxStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(decision.nextRetryAt())
                .isBetween(
                        before.plusSeconds(CommonConst.NEXT_RETRY_DELAY),
                        after.plusSeconds(CommonConst.NEXT_RETRY_DELAY)
                );
    }

    @Test
    @DisplayName("재시도 횟수가 1이면 대기 시간이 60초가 된다")
    void decide_재시도_횟수가_1이면_대기_시간이_60초가_된다() {
        // given
        int retryCount = 1;
        LocalDateTime before = LocalDateTime.now();

        // when
        RetryDecision decision = outboxRetryPolicy.decide(retryCount);

        LocalDateTime after = LocalDateTime.now();

        // then
        long expectedDelay = CommonConst.NEXT_RETRY_DELAY * 2L;

        assertThat(decision.outboxStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(decision.nextRetryAt())
                .isBetween(
                        before.plusSeconds(expectedDelay),
                        after.plusSeconds(expectedDelay)
                );
    }

    @Test
    @DisplayName("재시도 횟수가 2이면 대기 시간이 120초가 된다")
    void decide_재시도_횟수가_2이면_대기_시간이_120초가_된다() {
        // given
        int retryCount = 2;
        LocalDateTime before = LocalDateTime.now();

        // when
        RetryDecision decision = outboxRetryPolicy.decide(retryCount);

        LocalDateTime after = LocalDateTime.now();

        // then
        long expectedDelay = CommonConst.NEXT_RETRY_DELAY * 4L;

        assertThat(decision.outboxStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(decision.nextRetryAt())
                .isBetween(
                        before.plusSeconds(expectedDelay),
                        after.plusSeconds(expectedDelay)
                );
    }

    @Test
    @DisplayName("재시도 횟수가 4이면 대기 시간이 480초가 된다")
    void decide_재시도_횟수가_4이면_대기_시간이_480초가_된다() {
        // given
        int retryCount = CommonConst.MAX_RETRY - 1;
        LocalDateTime before = LocalDateTime.now();

        // when
        RetryDecision decision = outboxRetryPolicy.decide(retryCount);

        LocalDateTime after = LocalDateTime.now();

        // then
        long expectedDelay = CommonConst.NEXT_RETRY_DELAY * (1L << retryCount);

        assertThat(decision.outboxStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(decision.nextRetryAt())
                .isBetween(
                        before.plusSeconds(expectedDelay),
                        after.plusSeconds(expectedDelay)
                );
    }

    @Test
    @DisplayName("재시도 횟수가 최대 재시도 횟수보다 1 작으면 FAILED 상태를 반환한다")
    void decide_재시도_횟수가_MAX_RETRY보다_1_작으면_FAILED를_반환한다() {
        // given
        int retryCount = CommonConst.MAX_RETRY - 1;
        LocalDateTime before = LocalDateTime.now();

        // when
        RetryDecision decision = outboxRetryPolicy.decide(retryCount);

        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(decision.outboxStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(decision.nextRetryAt())
                .isNotNull()
                .isBetween(before, after.plusSeconds(MAX_DELAY_SECONDS));
    }

    @Test
    @DisplayName("재시도 횟수가 최대 재시도 횟수에 도달하면 DEAD 상태를 반환한다")
    void decide_재시도_횟수가_MAX_RETRY에_도달하면_DEAD를_반환한다() {
        // given
        int retryCount = CommonConst.MAX_RETRY;

        // when
        RetryDecision decision = outboxRetryPolicy.decide(retryCount);

        // then
        assertThat(decision.outboxStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(decision.nextRetryAt()).isNull();
        assertThat(decision.isDead()).isTrue();
    }

    @Test
    @DisplayName("재시도 횟수가 최대 재시도 횟수를 초과하면 DEAD 상태를 반환한다")
    void decide_재시도_횟수가_MAX_RETRY를_초과하면_DEAD를_반환한다() {
        // given
        int retryCount = CommonConst.MAX_RETRY + 1;

        // when
        RetryDecision decision = outboxRetryPolicy.decide(retryCount);

        // then
        assertThat(decision.outboxStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(decision.nextRetryAt()).isNull();
        assertThat(decision.isDead()).isTrue();
    }
}