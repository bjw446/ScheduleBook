package com.example.schedulebook.common.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AfterCommitExecutorTest {

    private AfterCommitExecutor afterCommitExecutor;

    @BeforeEach
    void setUp() {

        afterCommitExecutor =
                new AfterCommitExecutor();

        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("execute 호출만으로 Runnable이 즉시 실행되지 않는다")
    void givenAction_whenExecute_thenNotExecuteImmediately() {

        // given
        AtomicBoolean executed =
                new AtomicBoolean(false);

        Runnable action =
                () -> executed.set(true);

        // when
        afterCommitExecutor.execute(action);

        // then
        assertFalse(
                executed.get()
        );
    }

    @Test
    @DisplayName("Transaction commit 이후 Runnable이 실행된다")
    void givenAction_whenAfterCommit_thenExecuteAction() {

        // given
        AtomicBoolean executed =
                new AtomicBoolean(false);

        Runnable action =
                () -> executed.set(true);

        afterCommitExecutor.execute(action);

        // when
        TransactionSynchronizationManager
                .getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        // then
        assertTrue(
                executed.get()
        );
    }

    @Test
    @DisplayName("Transaction commit 이후 Runnable은 한 번 실행된다")
    void givenAction_whenAfterCommit_thenExecuteOnlyOnce() {

        // given
        AtomicInteger executionCount =
                new AtomicInteger(0);

        Runnable action =
                executionCount::incrementAndGet;

        afterCommitExecutor.execute(action);

        // when
        TransactionSynchronizationManager
                .getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        // then
        assertEquals(
                1,
                executionCount.get()
        );
    }

    @Test
    @DisplayName("commit 이후 Runnable에서 발생한 예외는 그대로 전파된다")
    void givenActionThrowsException_whenAfterCommit_thenPropagateException() {

        // given
        RuntimeException expected =
                new RuntimeException(
                        "after commit error"
                );

        Runnable action =
                () -> {
                    throw expected;
                };

        afterCommitExecutor.execute(action);

        TransactionSynchronization synchronization =
                TransactionSynchronizationManager
                        .getSynchronizations()
                        .get(0);

        // when
        RuntimeException actual =
                assertThrows(
                        RuntimeException.class,
                        synchronization::afterCommit
                );

        // then
        assertSame(
                expected,
                actual
        );
    }
}