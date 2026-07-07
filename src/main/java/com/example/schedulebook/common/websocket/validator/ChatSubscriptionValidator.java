package com.example.schedulebook.common.websocket.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chatroom.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.schedulebook.common.consts.WebSocketDestination.chatPrefix;

@Component
@RequiredArgsConstructor
public class ChatSubscriptionValidator implements SubscriptionValidator{
    private static final Pattern CHAT_PATTERN = Pattern.compile("^/topic/chat/(\\d+)(/.*)?$");
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public boolean supports(String destination) {
        return destination.startsWith(chatPrefix());
    }

    @Override
    public void validate(Long userId, String destination) {
        Matcher matcher = CHAT_PATTERN.matcher(destination);

        if (!matcher.matches()) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        Long roomId = Long.parseLong(matcher.group(1));

        chatRoomMemberRepository.findActiveByChatRoomIdAndUserId(roomId, userId).orElseThrow(
                () -> new BaseException(ErrorEnum.CHAT_ROOM_FORBIDDEN)
        );
    }
}
