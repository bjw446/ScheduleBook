package com.example.schedulebook.domain.chat.service;

import com.example.schedulebook.domain.chat.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import com.example.schedulebook.domain.chat.enums.SystemMessageType;
import com.example.schedulebook.domain.chat.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chat.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.schedulebook.domain.chat.consts.ChatConst.MAX_NAMES;

@Component
@RequiredArgsConstructor
public class ChatMessageManager {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatUnreadCountManager chatUnreadCountManager;

    public PublishChatMessage createLeaveSystemMessage(ChatRoom chatRoom, ChatRoomMember chatRoomMember){
        return createSystemMessage(
                chatRoom,
                SystemMessageType.USER_LEAVE,
                SystemMessageType.USER_LEAVE.format(chatRoomMember.getUser().getNickname())
        );
    }

    public PublishChatMessage createGroupRoomSystemMessage(ChatRoom chatRoom, User owner) {
        return createSystemMessage(
                chatRoom,
                SystemMessageType.GROUP_ROOM_CREATED,
                SystemMessageType.GROUP_ROOM_CREATED.format(owner.getNickname())
        );
    }

    public PublishChatMessage createInviteSystemMessage(ChatRoom chatRoom, User inviter, List<User> users) {
        String invitedNames = createInvitedNames(users);

        return createSystemMessage(
                chatRoom,
                SystemMessageType.USER_INVITED,
                SystemMessageType.USER_INVITED.format(
                        inviter.getNickname(),
                        invitedNames)
        );
    }

    public PublishChatMessage createUpdateNameSystemMessage(ChatRoom chatRoom, User user, String oldName, String newName) {
        return createSystemMessage(
                chatRoom,
                SystemMessageType.ROOM_NAME_UPDATED,
                SystemMessageType.ROOM_NAME_UPDATED.format(user.getNickname(), oldName, newName)
        );
    }

    public PublishChatMessage createScheduleUpdatedSystemMessage(ChatRoom chatRoom) {
        return createSystemMessage(
                chatRoom,
                SystemMessageType.SCHEDULE_UPDATED
        );
    }

    public PublishChatMessage createScheduleShareCanceledSystemMessage(ChatRoom chatRoom) {
        return createSystemMessage(
                chatRoom,
                SystemMessageType.SCHEDULE_SHARE_CANCELED
        );
    }

    public PublishChatMessage createScheduleDeletedSystemMessage(ChatRoom chatRoom) {
        return createSystemMessage(
                chatRoom,
                SystemMessageType.SCHEDULE_DELETED
        );
    }

    private PublishChatMessage createSystemMessage(ChatRoom chatRoom, SystemMessageType systemMessageType) {
        ChatMessage systemMessage = ChatMessage.system(chatRoom, systemMessageType);

        return createPublishMessage(chatRoom, systemMessage);
    }

    private PublishChatMessage createSystemMessage(ChatRoom chatRoom, SystemMessageType systemMessageType, String content) {
        ChatMessage systemMessage = ChatMessage.system(chatRoom, systemMessageType, content);

        return createPublishMessage(chatRoom, systemMessage);
    }

    private PublishChatMessage createPublishMessage(ChatRoom chatRoom, ChatMessage chatMessage) {
        chatRoom.updateLastMessage(chatMessage);

        chatMessageRepository.save(chatMessage);

        chatRoomMemberRepository.increaseUnreadCountAll(chatRoom.getId());

        List<MemberReadStatusProjection> readStatuses = chatRoomMemberRepository.findReadStatuses(chatRoom.getId());

        int unreadCount = chatUnreadCountManager.calculateUnreadCount(chatMessage, readStatuses);

        return new PublishChatMessage(chatMessage, unreadCount);
    }

    private String createInvitedNames(List<User> users) {
        if (users.size() <= MAX_NAMES) {
            return users.stream()
                    .map(user -> user.getNickname() + "님")
                    .collect(Collectors.joining(", "));
        }

        String firstUsers = users
                .stream()
                .limit(MAX_NAMES)
                .map(user -> user.getNickname() + "님")
                .collect(Collectors.joining(", "));

        return firstUsers + " 외 " + (users.size() - MAX_NAMES) + "명";
    }
}
