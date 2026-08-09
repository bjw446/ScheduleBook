package com.example.schedulebook.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RetrySchedulerMetrics {

    private final MeterRegistry meterRegistry;
    private final Set<String> registeredGauge = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, Counter> schedulerRunCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> processedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> retryCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> dlqCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    public void schedulerRun(String scheduler) {
        schedulerRunCounters.computeIfAbsent(scheduler, this::newSchedulerRunCounter).increment();
    }

    public void processed(String scheduler) {
        processedCounters.computeIfAbsent(scheduler, this::newProcessedCounter).increment();
    }

    public void success(String scheduler) {
        successCounters.computeIfAbsent(scheduler, this::newSuccessCounter).increment();
    }

    public void retry(String scheduler) {
        retryCounters.computeIfAbsent(scheduler, this::newRetryCounter).increment();
    }

    public void dlq(String scheduler) {
        dlqCounters.computeIfAbsent(scheduler, this::newDlqCounter).increment();
    }

    public void error(String scheduler) {
        errorCounters.computeIfAbsent(scheduler, this::newErrorCounter).increment();
    }

    public void registerPendingGauge(String scheduler, Supplier<Number> supplier) {
        if (!registeredGauge.add(scheduler)) {
            return;
        }

        try {
            Gauge.builder("retry.scheduler.pending", supplier, s -> s.get().doubleValue())
                    .description("현재 Retry 대기 건수")
                    .tag("scheduler", scheduler)
                    .register(meterRegistry);

            registeredGauge.add(scheduler);

        } catch (Exception e) {
            registeredGauge.remove(scheduler);

            throw e;
        }
    }

    private Counter newSchedulerRunCounter(String scheduler) {
        return Counter.builder("retry.scheduler.run")
                .description("Retry Scheduler 실행")
                .tag("scheduler", scheduler)
                .register(meterRegistry);
    }

    private Counter newProcessedCounter(String scheduler) {
        return Counter.builder("retry.scheduler.processed")
                .description("Retry 처리")
                .tag("scheduler", scheduler)
                .register(meterRegistry);
    }

    private Counter newSuccessCounter(String scheduler) {
        return Counter.builder("retry.scheduler.success")
                .description("Retry 성공")
                .tag("scheduler", scheduler)
                .register(meterRegistry);
    }

    private Counter newRetryCounter(String scheduler) {
        return Counter.builder("retry.scheduler.retry")
                .description("Retry 재시도")
                .tag("scheduler", scheduler)
                .register(meterRegistry);
    }

    private Counter newDlqCounter(String scheduler) {
        return Counter.builder("retry.scheduler.dlq")
                .description("Retry DLQ 저장")
                .tag("scheduler", scheduler)
                .register(meterRegistry);
    }

    private Counter newErrorCounter(String scheduler) {
        return Counter.builder("retry.scheduler.error")
                .description("Retry Scheduler 오류")
                .tag("scheduler", scheduler)
                .register(meterRegistry);
    }
}
