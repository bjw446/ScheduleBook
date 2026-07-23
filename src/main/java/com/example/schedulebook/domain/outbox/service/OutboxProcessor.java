package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import com.example.schedulebook.domain.outbox.publisher.OutboxPublisher;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OutboxProcessor {
    private final OutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;

    public void processPendingOutbox() {
        List<Outbox> outboxes = outboxRepository.findRetryTargets(
                LocalDateTime.now(),
                PageRequest.of(0, CommonConst.BATCH_SIZE)
        );

        for (Outbox outbox : outboxes) {
            try {
                outbox.processing();

                outboxPublisher.publish(outbox);

                outbox.success();

            } catch (Exception e) {
                log.error("Outbox 발행 실패 : {}", outbox.getId(), e);

                outbox.fail(e.getMessage(), LocalDateTime.now().plusMinutes(CommonConst.OUTBOX_RETRY_DELAY));
            }
        }
    }
}
