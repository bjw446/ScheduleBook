package com.example.schedulebook.domain.auth.metrics;

import com.example.schedulebook.domain.auth.repository.ForceLogoutRetryRepository;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;


@Component
public class ForceLogoutRetryMetrics {
    private final Counter schedulerRunCounter;
    private final Counter processedCounter;
    private final Counter successCounter;
    private final Counter retryCounter;
    private final Counter dlqCounter;
    private final Counter schedulerErrorCounter;

    public ForceLogoutRetryMetrics(MeterRegistry meterRegistry, ForceLogoutRetryRepository forceLogoutRetryRepository) {
        schedulerRunCounter = Counter.builder("force_logout.retry.scheduler.run")
                .description("강제 로그아웃 재시도 스케줄러 실행")
                .register(meterRegistry);

        processedCounter = Counter.builder("force_logout.retry.processed")
                .description("강제 로그아웃 재시도 처리")
                .register(meterRegistry);

        successCounter = Counter.builder("force_logout.retry.success")
                .description("강제 로그아웃 재시도 성공")
                .register(meterRegistry);

        retryCounter = Counter.builder("force_logout.retry.retry")
                .description("강제 로그아웃 재시도 스케줄러 재시도")
                .register(meterRegistry);

        dlqCounter = Counter.builder("force_logout.retry.dlq")
                .description("강제 로그아웃 재시도 DLQ 저장")
                .register(meterRegistry);

        schedulerErrorCounter = Counter.builder("force_logout.retry.scheduler.error")
                .description("강제 로그아웃 재시도 스케줄러 에러")
                .register(meterRegistry);

        Gauge.builder("force_logout.retry.pending", forceLogoutRetryRepository, ForceLogoutRetryRepository::countPending)
                .description("현재 재시도 대기중인 강제 로그아웃 개수")
                .register(meterRegistry);
    }

    public void schedulerRun() {
        schedulerRunCounter.increment();
    }

    public void processed() {
        processedCounter.increment();
    }

    public void success() {
        successCounter.increment();
    }

    public void retry() {
        retryCounter.increment();
    }

    public void dlq() {
        dlqCounter.increment();
    }

    public void schedulerError() {
        schedulerErrorCounter.increment();
    }
}
