package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleShareEventListener {
    private final ChatMessageRepository chatMessageRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSharePublisher scheduleSharePublisher;


    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleScheduleUpdated(ScheduleUpdatedEvent event) {
        Schedule schedule = scheduleRepository.findById(event.scheduleId()).orElseThrow();

        List<ChatMessage> chatMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(event.scheduleId())
                .stream()
                .filter(cm -> !cm.isScheduleShareCanceled())
                .toList();

        for (ChatMessage chatMessage : chatMessages) {
            chatMessage.updateScheduleSnapshot(schedule);
        }

        scheduleSharePublisher.publishScheduleUpdated(chatMessages);
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleScheduleCanceled(ScheduleDeletedEvent event) {
        List<ChatMessage> chatMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(event.scheduleId())
                .stream()
                .filter(cm -> !cm.isScheduleShareCanceled())
                .toList();

        for (ChatMessage chatMessage : chatMessages) {
            chatMessage.cancelScheduleShare();
        }

        scheduleSharePublisher.publishSharedScheduleDeleted(chatMessages);
    }
}
