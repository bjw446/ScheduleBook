package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void save(OutboxAggregateType aggregateType, Long aggregateId, OutboxEventType eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.create(
                    aggregateType,
                    aggregateId,
                    eventType,
                    json
            );

            outboxRepository.save(outbox);

        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorEnum.OUTBOX_PAYLOAD_SERIALIZATION_FAILED);

        } catch (Exception e) {
            log.error("Outbox 저장 실패 {}", e.getMessage(), e);

            throw e;
        }
    }

    public void delete(OutboxAggregateType aggregateType, Long aggregateId, OutboxEventType eventType) {
        outboxRepository.deleteByAggregateTypeAndAggregateIdAndEventType(
                aggregateType,
                aggregateId,
                eventType
        );
    }
}
