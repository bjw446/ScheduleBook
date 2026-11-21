package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OutboxServiceIntegrationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("동일한 eventId로 Outbox를 두 번 저장하면 유니크 제약 위반 예외가 발생한다")
    void save_동일한_eventId로_두번_저장하면_유니크_제약_위반_예외가_발생한다() {
        // given
        String eventId = UUID.randomUUID().toString();
        String aggregateId = "schedule-456";

        OutboxAggregateType aggregateType = OutboxAggregateType.SCHEDULE;
        OutboxEventType eventType = OutboxEventType.SCHEDULE_UPDATED;

        Map<String, Object> payload = Map.of(
                "scheduleId", 456
        );

        // 최초 Outbox 저장
        outboxService.save(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                payload
        );

        // 최초 데이터 저장 확인
        assertThat(outboxRepository.findAll())
                .anySatisfy(outbox ->
                        assertThat(outbox.getEventId()).isEqualTo(eventId)
                );

        // when & then
        assertThatThrownBy(() -> outboxService.save(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                payload
        ))
                .isInstanceOf(DataIntegrityViolationException.class);

        // then
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox WHERE event_id = ?",
                Integer.class,
                eventId
        );

        assertThat(count).isEqualTo(1);
    }
}