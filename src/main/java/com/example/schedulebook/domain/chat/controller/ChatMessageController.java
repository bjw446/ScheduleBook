package com.example.schedulebook.domain.chat.controller;

import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageSendRequest request) {
        chatMessageService.sendMessage(SecurityUtils.getCurrentUserId(), request);
    }
}
