package com.example.schedulebook.domain.scheduleshare.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleAttendanceResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.schedule.event.*;
import com.example.schedulebook.domain.schedule.service.ScheduleAccessValidator;
import com.example.schedulebook.domain.schedule.service.ScheduleParticipantReader;
import com.example.schedulebook.domain.scheduleshare.dto.request.UpdateAttendanceRequest;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleParticipant;
import com.example.schedulebook.domain.schedule.repository.ScheduleParticipantRepository;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.dto.request.ScheduleShareRequest;
import com.example.schedulebook.domain.scheduleshare.dto.response.*;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleShareService {
    private final ScheduleShareRepository scheduleShareRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleParticipantReader scheduleParticipantReader;
    private final ScheduleAttendancePublisher scheduleAttendancePublisher;
    private final ScheduleParticipantPublisher scheduleParticipantPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ScheduleAccessValidator scheduleAccessValidator;

    public ScheduleShareResponse shareSchedule(Long scheduleId, ScheduleShareRequest request, Long currentUserId) {
        validateUser(currentUserId);

        Schedule schedule = validateSchedule(scheduleId);

        validateScheduleOwner(schedule, currentUserId);

        User friendUser = validateUser(request.friendId());

        validateShareMyself(currentUserId, friendUser.getId());

        validateFriendRelation(currentUserId, friendUser.getId());

        ScheduleShare existing = scheduleShareRepository.findRelation(scheduleId, friendUser.getId()).orElse(null);

        if (existing != null) {
            validateShareStatus(existing);

            existing.reShare();

            eventPublisher.publishEvent(new ScheduleSharedEvent(friendUser.getId(), schedule.getUser().getNickname(), existing.getId()));

            return ScheduleShareResponse.from(existing);
        }

        try {
            ScheduleShare scheduleShare = ScheduleShare.create(schedule, friendUser);

            ScheduleShare savedScheduleShare = scheduleShareRepository.save(scheduleShare);

            createParticipants(schedule, List.of(friendUser));

            eventPublisher.publishEvent(new ScheduleSharedEvent(friendUser.getId(), schedule.getUser().getNickname(), savedScheduleShare.getId()));

            return ScheduleShareResponse.from(savedScheduleShare);

        } catch (DataIntegrityViolationException e) {
            ScheduleShare alreadyCreated = scheduleShareRepository.findRelation(scheduleId, friendUser.getId())
                    .orElseThrow(() -> e);

            validateShareStatus(alreadyCreated);

            alreadyCreated.reShare();

            eventPublisher.publishEvent(new ScheduleSharedEvent(friendUser.getId(), schedule.getUser().getNickname(), alreadyCreated.getId()));

            return ScheduleShareResponse.from(alreadyCreated);
        }
    }

    // 다른 사람에게 공유 받은 일정 목록 조회
    @Transactional(readOnly = true)
    public List<SharedScheduleResponse> findAllSharedSchedules(Long currentUserId) {
        validateUser(currentUserId);

        List<ScheduleShare> scheduleShares = scheduleShareRepository.findSharedSchedules(currentUserId, ScheduleShareStatus.ACTIVE);

        return scheduleShares.stream()
                .map(SharedScheduleResponse::from)
                .toList();
    }

    // 다른 사람에게 공유 받은 일정 상세 조회
    @Transactional(readOnly = true)
    public SharedScheduleDetailResponse findOneSharedSchedule(Long shareId, Long currentUserId) {
        validateUser(currentUserId);

        ScheduleShare scheduleShare = validateScheduleShare(shareId);

        validateSharedUser(scheduleShare, currentUserId);

        scheduleAccessValidator.validateAccessibleSchedule(scheduleShare.getSchedule().getId(), currentUserId);

        ScheduleParticipantInfo info = scheduleParticipantReader.getParticipantInfo(scheduleShare.getSchedule().getId(), currentUserId);

        return SharedScheduleDetailResponse.from(scheduleShare, info.participated(), info.participantCount(), info.participants());
    }

    // 내가 다른 사람에게 공유한 일정 목록 조회
    @Transactional(readOnly = true)
    public List<OwnedShareResponse> findAllOwnedShares(Long currentUserId) {
        validateUser(currentUserId);

        List<ScheduleShare> scheduleShares = scheduleShareRepository.findOwnedShares(currentUserId, ScheduleShareStatus.ACTIVE);

        return scheduleShares.stream()
                .map(OwnedShareResponse::from)
                .toList();
    }

    // 내가 다른 사람에게 공유한 일정 상세 조회
    @Transactional(readOnly = true)
    public OwnedShareDetailResponse findOneOwnedShareDetail(Long shareId, Long currentUserId) {
        validateUser(currentUserId);

        ScheduleShare scheduleShare = validateOwnedShare(shareId);

        validateShareOwner(scheduleShare, currentUserId);

        ScheduleParticipantInfo info = scheduleParticipantReader.getParticipantInfo(scheduleShare.getSchedule().getId(), currentUserId);

        return OwnedShareDetailResponse.from(scheduleShare, info.participated(), info.participantCount(), info.participants());
    }

    // 일정 참가자 목록 조회
    @Transactional(readOnly = true)
    public ScheduleParticipantListResponse findParticipants(Long currentUserId, Long scheduleId) {
        validateUser(currentUserId);

        scheduleAccessValidator.validateAccessibleSchedule(scheduleId, currentUserId);

        return scheduleParticipantReader.getParticipantList(scheduleId);
    }

    public void updateAttendance(Long currentUserId, Long scheduleId, UpdateAttendanceRequest request) {
        User user = validateUser(currentUserId);

        scheduleAccessValidator.validateAccessibleSchedule(scheduleId, currentUserId);

        ScheduleParticipant scheduleParticipant = scheduleParticipantRepository.findBySchedule_IdAndUser_Id(scheduleId, currentUserId)
                .orElseThrow(
                        () -> new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN)
                );

        if (scheduleParticipant.getAttendanceStatus() == request.attendanceStatus()) {
            return;
        }

        scheduleParticipant.updateAttendanceStatus(request.attendanceStatus());

        ScheduleAttendanceResponse response = ScheduleAttendanceResponse.of(scheduleId, currentUserId, user.getNickname(), request.attendanceStatus());

        scheduleAttendancePublisher.publishAttendanceUpdated(response);

        scheduleParticipantPublisher.publishParticipantsUpdated(scheduleId);
    }

    public void cancelShare(Long shareId, Long currentUserId) {
        validateUser(currentUserId);

        ScheduleShare scheduleShare = validateActiveScheduleShare(shareId);

        validateScheduleOwner(scheduleShare.getSchedule(), currentUserId);

        scheduleShare.cancelShare();

        applicationEventPublisher.publishEvent(new ScheduleCanceledEvent(scheduleShare.getSchedule().getId()));
    }

    private Schedule validateSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_NOT_FOUND)
        );
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }

        return user;
    }

    private void validateScheduleOwner(Schedule schedule, Long currentUserId) {
        if (!schedule.getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }

    private void validateShareMyself(Long currentUserId, Long friendId) {
        if (currentUserId.equals(friendId)) {
            throw new BaseException(ErrorEnum.CANNOT_SHARE_MYSELF);
        }
    }

    private void validateFriendRelation(Long currentUserId, Long friendId) {
        boolean exists = friendRepository.existsAcceptedFriend(currentUserId, friendId, FriendStatus.ACCEPTED);

        if (!exists) {
            throw new BaseException(ErrorEnum.FRIEND_NOT_FOUND);
        }
    }

    private void validateShareStatus(ScheduleShare scheduleShare) {
        if (scheduleShare.getScheduleShareStatus() == ScheduleShareStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.SCHEDULE_ALREADY_SHARED);
        }
    }

    private ScheduleShare validateScheduleShare(Long shareId) {
        return scheduleShareRepository.findActiveShareDetail(shareId, ScheduleShareStatus.ACTIVE).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND)
        );
    }

    private void validateSharedUser(ScheduleShare scheduleShare, Long currentUserId) {
        if (!scheduleShare.getSharedUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }

    private ScheduleShare validateActiveScheduleShare(Long shareId) {
        ScheduleShare scheduleShare = scheduleShareRepository.findByIdWithSchedule(shareId).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND)
        );

        if (scheduleShare.getScheduleShareStatus() != ScheduleShareStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.INVALID_SCHEDULE_SHARE_STATUS);
        }

        return scheduleShare;
    }

    private void validateShareOwner(ScheduleShare scheduleShare, Long currentUserId) {
        if (!scheduleShare.getSchedule().getUser().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }

    private ScheduleShare validateOwnedShare(Long shareId) {
        return scheduleShareRepository.findOwnedShareDetail(shareId, ScheduleShareStatus.ACTIVE).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_SHARE_NOT_FOUND)
        );
    }

    private void createParticipants(Schedule schedule, List<User> users) {
        Set<Long> participantIds = new HashSet<>(scheduleParticipantRepository.findParticipantIds(schedule.getId()));

        List<ScheduleParticipant> participants = users.stream()
                .filter(user -> !participantIds.contains(user.getId()))
                .map(user -> ScheduleParticipant.of(schedule, user))
                .toList();

        scheduleParticipantRepository.saveAll(participants);
    }
}