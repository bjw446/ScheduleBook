package com.example.schedulebook.common.security;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    private static final String TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final UserRole USER_ROLE = UserRole.USER;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private MessageChannel channel;

    private JwtChannelInterceptor jwtChannelInterceptor;

    @BeforeEach
    void setUp() {
        jwtChannelInterceptor = new JwtChannelInterceptor(jwtProvider);
    }

    @Test
    @DisplayName("STOMP accessor가 없으면 원본 Message를 반환한다")
    void givenMessageWithoutStompAccessor_whenPreSend_thenReturnOriginalMessage() {

        // given
        Message<String> message = MessageBuilder
                .withPayload("test")
                .build();

        // when
        Message<?> result =
                jwtChannelInterceptor.preSend(message, channel);

        // then
        assertSame(message, result);

        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("CONNECT가 아닌 STOMP Command는 인증하지 않는다")
    void givenNonConnectCommand_whenPreSend_thenDoNotAuthenticate() {

        // given
        Message<String> message =
                createStompMessage(StompCommand.SEND, null);

        // when
        Message<?> result =
                jwtChannelInterceptor.preSend(message, channel);

        // then
        assertSame(message, result);

        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("CONNECT에 Authorization Header가 없으면 TOKEN_MISSING으로 거부한다")
    void givenConnectWithoutAuthorization_whenPreSend_thenThrowTokenMissing() {

        // given
        Message<String> message =
                createStompMessage(StompCommand.CONNECT, null);

        // when
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtChannelInterceptor.preSend(message, channel)
        );

        // then
        assertEquals(
                ErrorEnum.TOKEN_MISSING,
                exception.getErrorEnum()
        );

        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("CONNECT에 Bearer가 아닌 Authorization Header가 있으면 TOKEN_MISSING으로 거부한다")
    void givenConnectWithNonBearerAuthorization_whenPreSend_thenThrowTokenMissing() {

        // given
        Message<String> message =
                createStompMessage(
                        StompCommand.CONNECT,
                        "Basic abcdef"
                );

        // when
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtChannelInterceptor.preSend(message, channel)
        );

        // then
        assertEquals(
                ErrorEnum.TOKEN_MISSING,
                exception.getErrorEnum()
        );

        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("정상적인 Bearer Token이면 STOMP 인증에 성공한다")
    void givenValidBearerToken_whenPreSend_thenAuthenticateUser() {

        // given
        Message<String> message =
                createConnectMessage();

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractUserRole(TOKEN))
                .thenReturn(USER_ROLE);

        // when
        Message<?> result =
                jwtChannelInterceptor.preSend(message, channel);

        // then
        assertSame(message, result);

        verify(jwtProvider)
                .validateToken(TOKEN);

        verify(jwtProvider)
                .extractUserId(TOKEN);

        verify(jwtProvider)
                .extractUserRole(TOKEN);

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(result);

        Authentication authentication =
                (Authentication) accessor.getUser();

        assertNotNull(authentication);

        assertInstanceOf(
                UsernamePasswordAuthenticationToken.class,
                authentication
        );

        assertInstanceOf(
                UserPrincipal.class,
                authentication.getPrincipal()
        );

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        assertEquals(
                USER_ID,
                principal.userId()
        );

        assertEquals(
                USER_ROLE,
                principal.userRole()
        );

        assertEquals(
                1,
                authentication.getAuthorities().size()
        );

        assertTrue(
                authentication.getAuthorities()
                        .contains(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
        );
    }

    @Test
    @DisplayName("JWT 검증에 실패하면 TOKEN_INVALID로 인증을 거부한다")
    void givenInvalidToken_whenPreSend_thenThrowTokenInvalid() {

        // given
        Message<String> message =
                createConnectMessage();

        doThrow(
                new BaseException(ErrorEnum.TOKEN_INVALID)
        )
                .when(jwtProvider)
                .validateToken(TOKEN);

        // when
        BaseException exception = assertThrows(
                BaseException.class,
                () -> jwtChannelInterceptor.preSend(message, channel)
        );

        // then
        assertEquals(
                ErrorEnum.TOKEN_INVALID,
                exception.getErrorEnum()
        );

        verify(jwtProvider)
                .validateToken(TOKEN);

        verify(jwtProvider, never())
                .extractUserId(anyString());

        verify(jwtProvider, never())
                .extractUserRole(anyString());
    }

    @Test
    @DisplayName("User ID와 User Role을 UserPrincipal에 저장한다")
    void givenValidToken_whenAuthenticate_thenStoreUserInfoInPrincipal() {

        // given
        Message<String> message =
                createConnectMessage();

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractUserRole(TOKEN))
                .thenReturn(USER_ROLE);

        // when
        Message<?> result =
                jwtChannelInterceptor.preSend(message, channel);

        // then
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(result);

        Authentication authentication =
                (Authentication) accessor.getUser();

        assertNotNull(authentication);

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        assertEquals(
                USER_ID,
                principal.userId()
        );

        assertEquals(
                USER_ROLE,
                principal.userRole()
        );
    }

    @Test
    @DisplayName("User Role에 맞는 ROLE_USER Authority를 생성한다")
    void givenValidUserRole_whenAuthenticate_thenCreateRoleUserAuthority() {

        // given
        Message<String> message =
                createConnectMessage();

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractUserRole(TOKEN))
                .thenReturn(USER_ROLE);

        // when
        Message<?> result =
                jwtChannelInterceptor.preSend(message, channel);

        // then
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(result);

        Authentication authentication =
                (Authentication) accessor.getUser();

        assertNotNull(authentication);

        assertTrue(
                authentication.getAuthorities()
                        .contains(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
        );
    }

    @Test
    @DisplayName("정상 인증 후 Authentication을 STOMP accessor에 저장한다")
    void givenValidToken_whenAuthenticate_thenSetAuthenticationToAccessor() {

        // given
        Message<String> message =
                createConnectMessage();

        when(jwtProvider.extractUserId(TOKEN))
                .thenReturn(USER_ID);

        when(jwtProvider.extractUserRole(TOKEN))
                .thenReturn(USER_ROLE);

        // when
        Message<?> result =
                jwtChannelInterceptor.preSend(message, channel);

        // then
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(result);

        assertNotNull(accessor.getUser());

        assertInstanceOf(
                UsernamePasswordAuthenticationToken.class,
                accessor.getUser()
        );
    }

    private Message<String> createConnectMessage() {

        return createStompMessage(
                StompCommand.CONNECT,
                "Bearer " + TOKEN
        );
    }

    private Message<String> createStompMessage(
            StompCommand command,
            String authorization
    ) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(command);

        if (authorization != null) {
            accessor.addNativeHeader(
                    HttpHeaders.AUTHORIZATION,
                    authorization
            );
        }

        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                "test",
                accessor.getMessageHeaders()
        );
    }
}