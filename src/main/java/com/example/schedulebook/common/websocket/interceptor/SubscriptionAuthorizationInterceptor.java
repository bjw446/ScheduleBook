package com.example.schedulebook.common.websocket.interceptor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.security.UserPrincipal;
import com.example.schedulebook.common.websocket.validator.SubscriptionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionAuthorizationInterceptor implements ChannelInterceptor {
    private final List<SubscriptionValidator> validators;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();

        if (destination == null) {
            return message;
        }

        Principal principal = accessor.getUser();

        Long userId = extractUserId(principal);

        for (SubscriptionValidator validator : validators) {
            if (validator.supports(destination)) {
                validator.validate(userId, destination);
                break;
            }
        }

        return message;
    }

    private Long extractUserId(Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)) {
            throw new BaseException(ErrorEnum.TOKEN_INVALID);
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BaseException(ErrorEnum.TOKEN_INVALID);
        }

        return userPrincipal.userId();
    }
}