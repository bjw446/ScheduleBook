package com.example.schedulebook.domain.chat.dto.response;

import java.util.List;

public record ChatRoomInviteResponse(
        Long roomId,
        List<Long> invitedUserIds
) {
}
