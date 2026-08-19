package com.example.schedulebook.domain.scheduleshare.service;

import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.service.OutboxService;
import com.example.schedulebook.domain.schedule.validator.ScheduleValidator;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleAttendanceResponse;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.scheduleparticipant.service.ScheduleParticipantReader;
import com.example.schedulebook.domain.scheduleparticipant.publisher.ScheduleAttendancePublisher;
import com.example.schedulebook.domain.scheduleparticipant.publisher.ScheduleParticipantPublisher;
import com.example.schedulebook.domain.scheduleparticipant.validator.ScheduleParticipantValidator;
import com.example.schedulebook.domain.scheduleshare.dto.request.UpdateAttendanceRequest;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.scheduleparticipant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleparticipant.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.scheduleshare.dto.request.ScheduleShareRequest;
import com.example.schedulebook.domain.scheduleshare.dto.response.*;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.schedule.event.ScheduleCanceledEvent;
import com.example.schedulebook.domain.scheduleshare.event.ScheduleSharedEvent;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.scheduleshare.validator.ScheduleShareValidator;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleShareService {
    private final ScheduleShareRepository scheduleShareRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleParticipantReader scheduleParticipantReader;
    private final ScheduleAttendancePublisher scheduleAttendancePublisher;
    private final ScheduleParticipantPublisher scheduleParticipantPublisher;
    private final UserValidator userValidator;
    private final ScheduleValidator scheduleValidator;
    private final ScheduleShareValidator scheduleShareValidator;
    private final FriendValidator friendValidator;
    private final ScheduleParticipantValidator scheduleParticipantValidator;
    private final OutboxService outboxService;

    public ScheduleShareResponse shareSchedule(Long scheduleId, ScheduleShareRequest request, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        Schedule schedule = scheduleValidator.validateSchedule(scheduleId, currentUserId);

        User friendUser = userValidator.validateActiveUser(request.friendId());

        userValidator.validateShareMyself(currentUserId, friendUser.getId());

        friendValidator.validateFriendRelation(currentUserId, friendUser.getId());

        ScheduleShare existing = scheduleShareRepository.findRelation(scheduleId, friendUser.getId()).orElse(null);

        if (existing != null) {
            scheduleShareValidator.validateShareStatus(existing);

            existing.reShare();

            return completeShare(schedule, friendUser, existing);
        }

        try {
            ScheduleShare scheduleShare = ScheduleShare.create(schedule, friendUser);

            ScheduleShare savedScheduleShare = scheduleShareRepository.save(scheduleShare);

            createParticipants(schedule, List.of(friendUser));

            return completeShare(schedule, friendUser, savedScheduleShare);

        } catch (DataIntegrityViolationException e) {
            ScheduleShare alreadyCreated = scheduleShareRepository.findRelation(scheduleId, friendUser.getId())
                    .orElseThrow(() -> e);

            scheduleShareValidator.validateShareStatus(alreadyCreated);

            alreadyCreated.reShare();

            return completeShare(schedule, friendUser, alreadyCreated);
        }
    }

    // 다른 사람에게 공유 받은 일정 목록 조회
    @Transactional(readOnly = true)
    public List<SharedScheduleResponse> findAllSharedSchedules(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<ScheduleShare> scheduleShares = scheduleShareRepository.findSharedSchedules(currentUserId, ScheduleShareStatus.ACTIVE);

        return scheduleShares.stream()
                .map(SharedScheduleResponse::from)
                .toList();
    }

    // 다른 사람에게 공유 받은 일정 상세 조회
    @Transactional(readOnly = true)
    public SharedScheduleDetailResponse findOneSharedSchedule(Long shareId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        ScheduleShare scheduleShare = scheduleShareValidator.validateScheduleShare(shareId);

        scheduleShareValidator.validateSharedUser(scheduleShare, currentUserId);

        scheduleValidator.validateAccessibleSchedule(scheduleShare.getSchedule().getId(), currentUserId);

        ScheduleParticipantInfo info = scheduleParticipantReader.getParticipantInfo(scheduleShare.getSchedule().getId(), currentUserId);

        return SharedScheduleDetailResponse.from(scheduleShare, info.participated(), info.participantCount(), info.participants());
    }

    // 내가 다른 사람에게 공유한 일정 목록 조회
    @Transactional(readOnly = true)
    public List<OwnedShareResponse> findAllOwnedShares(Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        List<ScheduleShare> scheduleShares = scheduleShareRepository.findOwnedShares(currentUserId, ScheduleShareStatus.ACTIVE);

        return scheduleShares.stream()
                .map(OwnedShareResponse::from)
                .toList();
    }

    // 내가 다른 사람에게 공유한 일정 상세 조회
    @Transactional(readOnly = true)
    public OwnedShareDetailResponse findOneOwnedShareDetail(Long shareId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        ScheduleShare scheduleShare = scheduleShareValidator.validateOwnedShare(shareId);

        scheduleShareValidator.validateShareOwner(scheduleShare, currentUserId);

        ScheduleParticipantInfo info = scheduleParticipantReader.getParticipantInfo(scheduleShare.getSchedule().getId(), currentUserId);

        return OwnedShareDetailResponse.from(scheduleShare, info.participated(), info.participantCount(), info.participants());
    }

    // 일정 참가자 목록 조회
    @Transactional(readOnly = true)
    public ScheduleParticipantListResponse findParticipants(Long currentUserId, Long scheduleId) {
        userValidator.validateActiveUser(currentUserId);

        scheduleValidator.validateAccessibleSchedule(scheduleId, currentUserId);

        return scheduleParticipantReader.getParticipantList(scheduleId);
    }

    public void updateAttendance(Long currentUserId, Long scheduleId, UpdateAttendanceRequest request) {
        User user = userValidator.validateActiveUser(currentUserId);

        scheduleValidator.validateAccessibleSchedule(scheduleId, currentUserId);

        ScheduleParticipant scheduleParticipant = scheduleParticipantValidator.validateParticipant(scheduleId, currentUserId);

        if (scheduleParticipant.getAttendanceStatus() == request.attendanceStatus()) {
            return;
        }

        scheduleParticipant.updateAttendanceStatus(request.attendanceStatus());

        ScheduleAttendanceResponse response = ScheduleAttendanceResponse.of(scheduleId, currentUserId, user.getNickname(), request.attendanceStatus());

        scheduleAttendancePublisher.publishAttendanceUpdated(response);

        scheduleParticipantPublisher.publishParticipantsUpdated(scheduleId);
    }

    public void cancelShare(Long shareId, Long currentUserId) {
        userValidator.validateActiveUser(currentUserId);

        ScheduleShare scheduleShare = scheduleShareValidator.validateActiveScheduleShare(shareId);

        scheduleValidator.validateScheduleOwner(scheduleShare.getSchedule(), currentUserId);

        scheduleShare.cancelShare();

        String eventId = UUID.randomUUID().toString();

        ScheduleCanceledEvent scheduleCanceledEvent = new ScheduleCanceledEvent(
                eventId,
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSharedUser().getId()
        );

        outboxService.save(
                eventId,
                OutboxAggregateType.SCHEDULE,
                String.valueOf(scheduleShare.getSchedule().getId()),
                OutboxEventType.SCHEDULE_CANCELED,
                scheduleCanceledEvent
        );
    }

    public void deleteAllShared(Long userId) {
        scheduleShareRepository.deleteAllBySharedUserId(userId);

        scheduleShareRepository.softDeleteOwnedShares(userId);
    }

    private void createParticipants(Schedule schedule, List<User> users) {
        Set<Long> participantIds = new HashSet<>(scheduleParticipantRepository.findParticipantIds(schedule.getId()));

        List<ScheduleParticipant> participants = users.stream()
                .filter(user -> !participantIds.contains(user.getId()))
                .map(user -> ScheduleParticipant.of(schedule, user))
                .toList();

        scheduleParticipantRepository.saveAll(participants);
    }

    private ScheduleShareResponse completeShare(Schedule schedule, User friendUser, ScheduleShare scheduleShare) {
        String eventId = UUID.randomUUID().toString();

        ScheduleSharedEvent scheduleSharedEvent = new ScheduleSharedEvent(
                eventId,
                friendUser.getId(),
                schedule.getUser().getNickname(),
                scheduleShare.getId()
        );

        outboxService.save(
                eventId,
                OutboxAggregateType.SCHEDULE,
                String.valueOf(schedule.getId()),
                OutboxEventType.SCHEDULE_SHARED,
                scheduleSharedEvent
        );

        return ScheduleShareResponse.from(scheduleShare);
    }
}