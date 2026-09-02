package com.example.schedulebook.common.websocket.interceptor;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.security.UserPrincipal;
import com.example.schedulebook.common.websocket.validator.SubscriptionValidator;
import com.example.schedulebook.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SubscriptionAuthorizationInterceptorTest {

    @Mock
    private SubscriptionValidator subscriptionValidator;

    @Mock
    private MessageChannel channel;

    private SubscriptionAuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SubscriptionAuthorizationInterceptor(
                List.of(subscriptionValidator)
        );
    }

    @Test
    void SUBSCRIBE가_아닌_메시지는_그대로_통과한다() {
        // given
        Message<String> message = createMessage(
                StompCommand.CONNECT,
                null,
                null
        );

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertSame(message, result);
        verifyNoInteractions(subscriptionValidator);
    }

    @Test
    void StompHeaderAccessor를_가져올_수_없으면_그대로_통과한다() {
        // given
        Message<String> message = MessageBuilder
                .withPayload("test")
                .build();

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertSame(message, result);
        verifyNoInteractions(subscriptionValidator);
    }

    @Test
    void destination이_없으면_그대로_통과한다() {
        // given
        Message<String> message = createMessage(
                StompCommand.SUBSCRIBE,
                null,
                null
        );

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertSame(message, result);
        verifyNoInteractions(subscriptionValidator);
    }

    @Test
    void principal이_없으면_TOKEN_INVALID를_던진다() {
        // given
        String destination = "/user/queue/notification";

        Message<String> message = createMessage(
                StompCommand.SUBSCRIBE,
                destination,
                null
        );

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> interceptor.preSend(message, channel)
        );

        assertEquals(ErrorEnum.TOKEN_INVALID, exception.getErrorEnum());
        verifyNoInteractions(subscriptionValidator);
    }

    @Test
    void 지원하지_않는_destination이면_validator를_호출하지_않는다() {
        // given
        Long userId = 1L;
        String destination = "/unsupported";

        Message<String> message = createMessage(
                StompCommand.SUBSCRIBE,
                destination,
                createAuthentication(userId)
        );

        given(subscriptionValidator.supports(destination))
                .willReturn(false);

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertSame(message, result);

        verify(subscriptionValidator).supports(destination);
        verify(subscriptionValidator, never())
                .validate(anyLong(), anyString());
    }

    @Test
    void 허용되는_destination이면_validator에_검증을_위임한다() {
        // given
        Long userId = 1L;
        String destination = "/user/queue/notification";

        Message<String> message = createMessage(
                StompCommand.SUBSCRIBE,
                destination,
                createAuthentication(userId)
        );

        given(subscriptionValidator.supports(destination))
                .willReturn(true);

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertSame(message, result);

        verify(subscriptionValidator).supports(destination);
        verify(subscriptionValidator).validate(userId, destination);
    }

    @Test
    void 여러_validator_중_지원하는_validator만_검증한다() {
        // given
        SubscriptionValidator firstValidator =
                mock(SubscriptionValidator.class);

        SubscriptionValidator secondValidator =
                mock(SubscriptionValidator.class);

        interceptor = new SubscriptionAuthorizationInterceptor(
                List.of(firstValidator, secondValidator)
        );

        Long userId = 1L;
        String destination = "/user/queue/notification";

        Message<String> message = createMessage(
                StompCommand.SUBSCRIBE,
                destination,
                createAuthentication(userId)
        );

        given(firstValidator.supports(destination))
                .willReturn(false);

        given(secondValidator.supports(destination))
                .willReturn(true);

        // when
        Message<?> result = interceptor.preSend(message, channel);

        // then
        assertSame(message, result);

        verify(firstValidator).supports(destination);
        verify(firstValidator, never())
                .validate(anyLong(), anyString());

        verify(secondValidator).supports(destination);
        verify(secondValidator).validate(userId, destination);
    }

    @Test
    void validator_검증에_실패하면_예외를_그대로_전파한다() {
        // given
        Long userId = 1L;
        String destination = "/user/queue/notification";

        Message<String> message = createMessage(
                StompCommand.SUBSCRIBE,
                destination,
                createAuthentication(userId)
        );

        BaseException exception = new BaseException(
                ErrorEnum.TOKEN_INVALID
        );

        given(subscriptionValidator.supports(destination))
                .willReturn(true);

        willThrow(exception)
                .given(subscriptionValidator)
                .validate(userId, destination);

        // when & then
        BaseException thrown = assertThrows(
                BaseException.class,
                () -> interceptor.preSend(message, channel)
        );

        assertSame(exception, thrown);

        verify(subscriptionValidator).supports(destination);
        verify(subscriptionValidator).validate(userId, destination);
    }

    @Test
    void AuthenticationToken의_principal이_UserPrincipal이_아니면_TOKEN_INVALID를_던진다() {
        // given
        String destination = "/user/queue/notification";

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "invalid-principal",
                        null
                );

        Message<String> message = createMessage(
                StompCommand.SUBSCRIBE,
                destination,
                authentication
        );

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> interceptor.preSend(message, channel)
        );

        assertEquals(
                ErrorEnum.TOKEN_INVALID,
                exception.getErrorEnum()
        );

        verifyNoInteractions(subscriptionValidator);
    }

    private Message<String> createMessage(
            StompCommand command,
            String destination,
            Principal principal
    ) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(command);

        if (destination != null) {
            accessor.setDestination(destination);
        }

        if (principal != null) {
            accessor.setUser(principal);
        }

        return MessageBuilder.createMessage(
                "test",
                accessor.getMessageHeaders()
        );
    }

    private UsernamePasswordAuthenticationToken createAuthentication(
            Long userId
    ) {
        UserPrincipal userPrincipal = new UserPrincipal(
                userId,
                UserRole.USER
        );

        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of()
        );
    }
}