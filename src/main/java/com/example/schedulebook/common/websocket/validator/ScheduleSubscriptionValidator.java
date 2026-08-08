package com.example.schedulebook.common.websocket.validator;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.scheduleshare.enums.ScheduleShareStatus;
import com.example.schedulebook.domain.scheduleshare.repository.ScheduleShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ScheduleSubscriptionValidator implements SubscriptionValidator{
    private static final Pattern PATTERN = Pattern.compile("^/topic/schedule/(\\d+)(/.*)?$");
    private final ScheduleRepository scheduleRepository;
    private final ScheduleShareRepository scheduleShareRepository;

    @Override
    public boolean supports(String destination) {
        return destination.startsWith(WebSocketDestination.getSchedulePrefix());
    }

    @Override
    public void validate(Long userId, String destination) {
        Matcher matcher = PATTERN.matcher(destination);

        if (!matcher.matches()) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        Long scheduleId = Long.parseLong(matcher.group(1));

        if (scheduleRepository.existsByIdAndUser_Id(scheduleId, userId)) {
            return;
        }

        scheduleShareRepository.findActiveRelation(scheduleId, userId, ScheduleShareStatus.ACTIVE).orElseThrow(
                () -> new BaseException(ErrorEnum.SCHEDULE_FORBIDDEN)
        );
    }
}