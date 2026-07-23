package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.outbox.entity.Outbox;
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
        recoverStuck();

        List<Outbox> outboxes = claimOutboxes();

        for (Outbox outbox : outboxes) {
            try {
                outboxPublisher.publish(outbox);

                outbox.success();

            } catch (Exception e) {
                outbox.increaseRetryCount();

                if (outbox.getRetryCount() >= CommonConst.MAX_RETRY) {
                    outbox.dead();

                    log.error("Outbox {} 영구 실패", outbox.getId(), e);
                } else {
                    outbox.fail(e.getMessage(), LocalDateTime.now().plusMinutes(CommonConst.OUTBOX_RETRY_DELAY));

                    log.error("Outbox 발행 실패 : {}", outbox.getId(), e);
                }
            }
        }
    }

    private List<Outbox> claimOutboxes() {
        List<Outbox> outboxes = outboxRepository.findRetryTargets(
                LocalDateTime.now(),
                PageRequest.of(0, CommonConst.BATCH_SIZE)
        );

        outboxes.forEach(Outbox::processing);

        outboxRepository.flush();

        return outboxes;
    }

    private void recoverStuck() {
        List<Outbox> outboxes = outboxRepository.findStuckOutboxes(LocalDateTime.now().minusMinutes(30));

        for (Outbox outbox : outboxes) {
            outbox.increaseRetryCount();

            if (outbox.getRetryCount() >= CommonConst.MAX_RETRY) {
                outbox.dead();

                log.error("Outbox {} lease timeout으로 DEAD 처리", outbox.getId());

            } else {
                outbox.fail(
                        "Outbox 처리 중 시간 초과",
                        LocalDateTime.now().plusMinutes(CommonConst.OUTBOX_RETRY_DELAY)
                );
            }
        }
    }
}
