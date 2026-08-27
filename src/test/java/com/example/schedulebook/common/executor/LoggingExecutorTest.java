package com.example.schedulebook.common.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LoggingExecutorTest {

    private LoggingExecutor loggingExecutor;

    @BeforeEach
    void setUp() {

        loggingExecutor =
                new LoggingExecutor();
    }

    @Test
    @DisplayName("Runnable이 정상 실행되면 true를 반환한다")
    void givenValidAction_whenExecute_thenReturnTrue() {

        // given
        Long outboxId =
                1L;

        String name =
                "Dashboard Update";

        Runnable action =
                () -> {
                };

        // when
        boolean result =
                loggingExecutor.execute(
                        outboxId,
                        name,
                        action
                );

        // then
        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("Runnable이 정상적으로 실행된다")
    void givenAction_whenExecute_thenRunAction() {

        // given
        AtomicBoolean executed =
                new AtomicBoolean(false);

        Runnable action =
                () -> executed.set(true);

        // when
        loggingExecutor.execute(
                1L,
                "Dashboard Update",
                action
        );

        // then
        assertTrue(
                executed.get()
        );
    }

    @Test
    @DisplayName("Runnable 실행 중 예외가 발생하면 false를 반환한다")
    void givenActionThrowsException_whenExecute_thenReturnFalse() {

        // given
        Runnable action =
                () -> {
                    throw new RuntimeException(
                            "execution failed"
                    );
                };

        // when
        boolean result =
                loggingExecutor.execute(
                        1L,
                        "Dashboard Update",
                        action
                );

        // then
        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Runnable에서 발생한 예외를 외부로 전파하지 않는다")
    void givenActionThrowsException_whenExecute_thenNotPropagateException() {

        // given
        Runnable action =
                () -> {
                    throw new RuntimeException(
                            "execution failed"
                    );
                };

        // when & then
        assertDoesNotThrow(
                () -> loggingExecutor.execute(
                        1L,
                        "Dashboard Update",
                        action
                )
        );
    }
}