package com.example.schedulebook.domain.chat.controller;

import com.example.schedulebook.common.security.UserPrincipal;
import com.example.schedulebook.domain.chat.dto.request.ChatMessageSendRequest;
import com.example.schedulebook.domain.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid ChatMessageSendRequest request, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal)authentication.getPrincipal();

        chatMessageService.sendMessage(userPrincipal.userId(), request);
    }
}
