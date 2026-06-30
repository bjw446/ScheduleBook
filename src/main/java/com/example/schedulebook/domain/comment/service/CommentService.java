package com.example.schedulebook.domain.comment.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.comment.repository.CommentRepository;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    private boolean isScheduleAccessible(Schedule schedule, Long currentUserId) {
        return schedule.getUser().getId().equals(currentUserId)
                || scheduleParticipantRepository.existsBySchedule_IdAndUser_Id(
                schedule.getId(), currentUserId);
    }

    private void validateScheduleAccessible(Schedule schedule, Long currentUserId) {
        if (!isScheduleAccessible(schedule, currentUserId)) {
            throw new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN);
        }
    }
}
