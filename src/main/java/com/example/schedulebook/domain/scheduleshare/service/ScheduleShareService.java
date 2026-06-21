package com.example.schedulebook.domain.scheduleshare.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.friend.repository.FriendRepository;
import com.example.schedulebook.domain.notification.consts.NotificationMessages;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.notification.service.NotificationService;
import com.example.schedulebook.domain.schedule.entity.Schedule;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleShareService {
    private final ScheduleShareRepository scheduleShareRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final NotificationService notificationService;

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

            notificationService.createNotification(
                    friendUser.getId(),
                    NotificationType.SCHEDULE_SHARED,
                    NotificationMessages.SCHEDULE_SHARED,
                    schedule.getUser().getNickname() + NotificationMessages.SCHEDULE_SHARED_MESSAGE,
                    existing.getId()
            );

            return ScheduleShareResponse.from(existing);
        }

        try {
            ScheduleShare scheduleShare = ScheduleShare.create(schedule, friendUser);

            ScheduleShare savedScheduleShare = scheduleShareRepository.save(scheduleShare);

            notificationService.createNotification(
                    friendUser.getId(),
                    NotificationType.SCHEDULE_SHARED,
                    NotificationMessages.SCHEDULE_SHARED,
                    schedule.getUser().getNickname() + NotificationMessages.SCHEDULE_SHARED_MESSAGE,
                    savedScheduleShare.getId()
            );

            return ScheduleShareResponse.from(savedScheduleShare);

        } catch (DataIntegrityViolationException e) {
            ScheduleShare alreadyCreated = scheduleShareRepository.findRelation(scheduleId, friendUser.getId())
                    .orElseThrow(() -> e);

            validateShareStatus(alreadyCreated);

            alreadyCreated.reShare();

            notificationService.createNotification(
                    friendUser.getId(),
                    NotificationType.SCHEDULE_SHARED,
                    NotificationMessages.SCHEDULE_SHARED,
                    schedule.getUser().getNickname() + NotificationMessages.SCHEDULE_SHARED_MESSAGE,
                    alreadyCreated.getId()
            );

            return ScheduleShareResponse.from(alreadyCreated);
        }
    }

    @Transactional(readOnly = true)
    public List<SharedScheduleResponse> findAllSharedSchedules(Long currentUserId) {
        validateUser(currentUserId);

        List<ScheduleShare> scheduleShares = scheduleShareRepository.findSharedSchedules(currentUserId, ScheduleShareStatus.ACTIVE);

        return scheduleShares.stream()
                .map(SharedScheduleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SharedScheduleDetailResponse findOneSharedSchedule(Long shareId, Long currentUserId) {
        validateUser(currentUserId);

        ScheduleShare scheduleShare = validateScheduleShare(shareId);

        validateSharedUser(scheduleShare, currentUserId);

        return SharedScheduleDetailResponse.from(scheduleShare);
    }

    @Transactional(readOnly = true)
    public List<OwnedShareResponse> findAllOwnedShares(Long currentUserId) {
        validateUser(currentUserId);

        List<ScheduleShare> scheduleShares = scheduleShareRepository.findOwnedShares(currentUserId, ScheduleShareStatus.ACTIVE);

        return scheduleShares.stream()
                .map(OwnedShareResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnedShareDetailResponse findOneOwnedShareDetail(Long shareId, Long currentUserId) {
        validateUser(currentUserId);

        ScheduleShare scheduleShare = validateOwnedShare(shareId);

        validateShareOwner(scheduleShare, currentUserId);

        return OwnedShareDetailResponse.from(scheduleShare);
    }

    public void cancelShare(Long shareId, Long currentUserId) {
        validateUser(currentUserId);

        ScheduleShare scheduleShare = validateActiveScheduleShare(shareId);

        validateScheduleOwner(scheduleShare.getSchedule(), currentUserId);

        scheduleShare.cancelShare();
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
}