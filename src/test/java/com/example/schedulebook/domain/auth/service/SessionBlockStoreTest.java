package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionBlockStoreTest {

    @Mock
    private TaskScheduler sessionBlockTaskScheduler;

    @InjectMocks
    private SessionBlockStore sessionBlockStore;

    private String sessionId;
    private long expiration;

    @BeforeEach
    void setUp() {
        sessionId = "session-1";
        expiration = 60_000L;
    }

    @Test
    void given정상세션과유효한만료시간_whenBlock_then세션차단및해제작업예약() {
        // given
        ArgumentCaptor<Runnable> taskCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> expireAtCaptor =
                ArgumentCaptor.forClass(Instant.class);

        Instant before = Instant.now();

        // when
        sessionBlockStore.block(sessionId, expiration);

        // then
        Instant after = Instant.now();

        assertThat(sessionBlockStore.isBlocked(sessionId))
                .isTrue();

        verify(sessionBlockTaskScheduler).schedule(
                taskCaptor.capture(),
                expireAtCaptor.capture()
        );

        Instant expireAt = expireAtCaptor.getValue();

        assertThat(expireAt)
                .isBetween(
                        before.plusMillis(expiration),
                        after.plusMillis(expiration)
                );

        // 예약된 해제 작업 실행
        taskCaptor.getValue().run();

        assertThat(sessionBlockStore.isBlocked(sessionId))
                .isFalse();
    }

    @Test
    void givenNullSessionId_whenBlock_thenInvalidInput예외() {
        // when & then
        assertThatThrownBy(() ->
                sessionBlockStore.block(null, expiration)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verifyNoInteractions(sessionBlockTaskScheduler);
    }

    @Test
    void givenBlankSessionId_whenBlock_thenInvalidInput예외() {
        // when & then
        assertThatThrownBy(() ->
                sessionBlockStore.block("   ", expiration)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verifyNoInteractions(sessionBlockTaskScheduler);
    }

    @Test
    void given만료시간이0이하_whenBlock_thenInvalidInput예외() {
        // when & then
        assertThatThrownBy(() ->
                sessionBlockStore.block(sessionId, 0)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        assertThatThrownBy(() ->
                sessionBlockStore.block(sessionId, -1)
        )
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_INPUT);

        verifyNoInteractions(sessionBlockTaskScheduler);
    }

    @Test
    void given차단되지않은세션_whenIsBlocked_thenFalse() {
        // when & then
        assertThat(sessionBlockStore.isBlocked(sessionId))
                .isFalse();

        verifyNoInteractions(sessionBlockTaskScheduler);
    }

    @Test
    void given정상적으로차단된세션_whenIsBlocked_thenTrue() {
        // given
        sessionBlockStore.block(sessionId, expiration);

        // when
        boolean result = sessionBlockStore.isBlocked(sessionId);

        // then
        assertThat(result)
                .isTrue();

        verify(sessionBlockTaskScheduler).schedule(
                any(Runnable.class),
                any(Instant.class)
        );
    }

    @Test
    void givenNull또는BlankSessionId_whenIsBlocked_thenFalse() {
        // when & then
        assertThat(sessionBlockStore.isBlocked(null))
                .isFalse();

        assertThat(sessionBlockStore.isBlocked(""))
                .isFalse();

        assertThat(sessionBlockStore.isBlocked("   "))
                .isFalse();

        verifyNoInteractions(sessionBlockTaskScheduler);
    }

    @Test
    void given차단된세션_whenUnblock_then차단해제() {
        // given
        sessionBlockStore.block(sessionId, expiration);

        assertThat(sessionBlockStore.isBlocked(sessionId))
                .isTrue();

        // when
        sessionBlockStore.unblock(sessionId);

        // then
        assertThat(sessionBlockStore.isBlocked(sessionId))
                .isFalse();
    }

    @Test
    void given존재하지않는세션_whenUnblock_then예외없이종료() {
        // when & then
        assertThatCode(() ->
                sessionBlockStore.unblock(sessionId)
        )
                .doesNotThrowAnyException();
    }
}