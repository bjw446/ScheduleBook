package com.example.schedulebook.domain.auth.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.auth.entity.AuditLog;
import com.example.schedulebook.domain.auth.enums.AuditEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("이미 처리한 Outbox ID인지 확인한다")
    void existsByOutboxId_존재하는_Outbox_ID를_확인한다() {
        // given
        AuditLog auditLog = createAuditLog(1L);

        auditLogRepository.save(auditLog);

        // when
        boolean exists = auditLogRepository.existsByOutboxId(1L);
        boolean notExists = auditLogRepository.existsByOutboxId(999L);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("서로 다른 Outbox ID를 구분하여 조회한다")
    void existsByOutboxId_서로_다른_Outbox_ID를_구분한다() {
        // given
        AuditLog firstAuditLog = createAuditLog(1L);
        AuditLog secondAuditLog = createAuditLog(2L);

        auditLogRepository.save(firstAuditLog);
        auditLogRepository.save(secondAuditLog);

        // when
        boolean firstExists = auditLogRepository.existsByOutboxId(1L);
        boolean secondExists = auditLogRepository.existsByOutboxId(2L);
        boolean thirdExists = auditLogRepository.existsByOutboxId(3L);

        // then
        assertThat(firstExists).isTrue();
        assertThat(secondExists).isTrue();
        assertThat(thirdExists).isFalse();
    }

    @Test
    @DisplayName("동일한 Outbox ID로 감사 로그를 중복 저장할 수 없다")
    void save_동일한_Outbox_ID로_중복_저장할_수_없다() {
        // given
        AuditLog firstAuditLog = createAuditLog(1L);
        AuditLog secondAuditLog = createAuditLog(1L);

        auditLogRepository.saveAndFlush(firstAuditLog);

        // when & then
        assertThatThrownBy(() -> auditLogRepository.saveAndFlush(secondAuditLog))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AuditLog createAuditLog(Long outboxId) {
        return AuditLog.create(
                1L,
                null,
                "testLogin",
                AuditEventType.LOGIN_SUCCESS,
                "로그인 성공",
                "127.0.0.1",
                "Test User Agent",
                outboxId
        );
    }
}