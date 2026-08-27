package com.example.schedulebook.common.executor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LoggingExecutorTest {

    private LoggingExecutor loggingExecutor;

    private Logger logger;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {

        loggingExecutor =
                new LoggingExecutor();

        logger =
                (Logger) LoggerFactory.getLogger(
                        LoggingExecutor.class
                );

        listAppender =
                new ListAppender<>();

        listAppender.start();

        logger.addAppender(
                listAppender
        );
    }

    @AfterEach
    void tearDown() {

        logger.detachAppender(
                listAppender
        );

        listAppender.stop();
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

    @Test
    @DisplayName("실패 시 outboxId, name, 예외 메시지가 error 로그에 기록된다")
    void givenActionThrowsException_whenExecute_thenLogError() {

        // given
        Long outboxId =
                100L;

        String name =
                "Dashboard Update";

        String exceptionMessage =
                "execution failed";

        Runnable action =
                () -> {
                    throw new RuntimeException(
                            exceptionMessage
                    );
                };

        // when
        boolean result =
                loggingExecutor.execute(
                        outboxId,
                        name,
                        action
                );

        // then
        assertFalse(
                result
        );

        assertEquals(
                1,
                listAppender.list.size()
        );

        ILoggingEvent logEvent =
                listAppender.list.get(0);

        assertEquals(
                Level.ERROR,
                logEvent.getLevel()
        );

        assertTrue(
                logEvent.getFormattedMessage()
                        .contains(name)
        );

        assertTrue(
                logEvent.getFormattedMessage()
                        .contains(exceptionMessage)
        );

        assertTrue(
                logEvent.getFormattedMessage()
                        .contains(outboxId.toString())
        );

        assertNotNull(
                logEvent.getThrowableProxy()
        );
    }
}