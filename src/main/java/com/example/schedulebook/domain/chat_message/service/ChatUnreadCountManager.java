package com.example.schedulebook.domain.chat_message.service;

import com.example.schedulebook.domain.chat_message.entity.ChatMessage;
import com.example.schedulebook.domain.chat_room.entity.ChatRoomMember;
import com.example.schedulebook.domain.chat_room.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chat_message.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatUnreadCountManager {
    private final ChatMessageRepository chatMessageRepository;

    public int calculateUnreadCount(ChatMessage chatMessage, List<MemberReadStatusProjection> members) {
        Long senderId = chatMessage.getSender() == null ? null : chatMessage.getSender().getId();

        return (int) members.stream()
                .filter(member ->
                        senderId == null
                                ||
                                !member.getUserId().equals(senderId)
                )

                .filter(member ->
                        member.getLastReadMessageId() == null
                                ||
                                member.getLastReadMessageId()
                                        < chatMessage.getId()
                )

                .filter(member ->
                        !chatMessage.getCreatedAt()
                                .isBefore(member.getJoinedAt())
                )

                .count();
    }

    public int calculateUnreadCount(ChatMessage chatMessage, Long excludedUserId, List<MemberReadStatusProjection> members) {
        return (int) members.stream()
                .filter(member ->
                        !member.getUserId().equals(excludedUserId)
                )
                .filter(member ->
                        member.getLastReadMessageId() == null
                                || member.getLastReadMessageId() < chatMessage.getId()
                )
                .filter(member ->
                        !chatMessage.getCreatedAt().isBefore(member.getJoinedAt()))
                .count();
    }

    public long recalculateUnreadCount(ChatRoomMember chatRoomMember) {
        return chatMessageRepository.countUnreadMessages(
                chatRoomMember.getChatRoom().getId(),

                chatRoomMember.getLastReadMessageId() == null
                        ? 0L
                        : chatRoomMember.getLastReadMessageId(),

                chatRoomMember.getUser().getId(),

                chatRoomMember.getJoinedAt()
        );
    }
}
