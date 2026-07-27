package com.example.schedulebook.domain.comment.event;

import com.example.schedulebook.domain.comment.dto.response.CommentEventResponse;
import com.example.schedulebook.domain.comment.enums.CommentEventType;

public record CommentEvent(
        CommentEventType commentEventType,
        CommentEventResponse commentEventResponse
) {
    public static CommentEvent from(CommentEventResponse commentEventResponse) {
        return new CommentEvent(
                commentEventResponse.commentEventType(),
                commentEventResponse
        );
    }
}
