package com.example.schedulebook.domain.chat.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSearchRequest;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chat.dto.request.ChatReadRequest;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageSliceResponse;
import com.example.schedulebook.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageSendRequest request) {
        chatMessageService.sendMessage(SecurityUtils.getCurrentUserId(), request);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageSliceResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") Integer size
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        chatMessageService.findMessages(
                                SecurityUtils.getCurrentUserId(),
                                roomId,
                                new ChatMessageSearchRequest(cursor, size)
                        )
                )
        );
    }

    @PostMapping("/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessage(@PathVariable Long roomId, @RequestBody ChatReadRequest request) {
        chatMessageService.readMessage(SecurityUtils.getCurrentUserId(), roomId, request.lastReadMessageId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        null
                )
        );
    }
}
