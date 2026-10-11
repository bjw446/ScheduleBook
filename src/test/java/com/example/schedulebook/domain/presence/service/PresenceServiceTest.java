package com.example.schedulebook.domain.presence.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.service.RedisPresenceService;
import com.example.schedulebook.domain.friend.validator.FriendValidator;
import com.example.schedulebook.domain.presence.dto.response.UserPresenceResponse;
import com.example.schedulebook.domain.user.validator.UserValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenceService 테스트")
class PresenceServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @Mock
    private UserValidator userValidator;

    @Mock
    private FriendValidator friendValidator;

    @Mock
    private RedisPresenceService redisPresenceService;

    @InjectMocks
    private PresenceService presenceService;

    @Test
    @DisplayName("자기 자신의 접속 상태를 조회한다")
    void findPresence_자기_자신의_접속_상태를_조회한다() {
        // given
        when(redisPresenceService.getSessionCount(CURRENT_USER_ID))
                .thenReturn(1);

        // when
        UserPresenceResponse response =
                presenceService.findPresence(CURRENT_USER_ID, CURRENT_USER_ID);

        // then
        assertThat(response.userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(response.online()).isTrue();
        assertThat(response.activeSessions()).isEqualTo(1);


        verify(userValidator).validateActiveUser(CURRENT_USER_ID);
        verifyNoInteractions(friendValidator);
        verify(redisPresenceService).getSessionCount(CURRENT_USER_ID);
    }

    @Test
    @DisplayName("친구의 접속 상태를 조회한다")
    void findPresence_친구의_접속_상태를_조회한다() {
        // given
        when(redisPresenceService.getSessionCount(TARGET_USER_ID))
                .thenReturn(1);

        // when
        UserPresenceResponse response =
                presenceService.findPresence(CURRENT_USER_ID, TARGET_USER_ID);

        // then
        assertThat(response.userId()).isEqualTo(TARGET_USER_ID);
        assertThat(response.online()).isTrue();
        assertThat(response.activeSessions()).isEqualTo(1);

        verify(friendValidator)
                .validatePresenceAccess(CURRENT_USER_ID, TARGET_USER_ID);
        verify(userValidator).validateActiveUser(TARGET_USER_ID);
        verify(redisPresenceService).getSessionCount(TARGET_USER_ID);
    }

    @Test
    @DisplayName("비친구의 접속 상태 조회 권한이 없으면 예외가 발생한다")
    void findPresence_비친구의_접속_상태_조회_권한이_없으면_예외가_발생한다() {
        // given
        doThrow(new BaseException(ErrorEnum.PRESENCE_ACCESS_DENIED))
                .when(friendValidator)
                .validatePresenceAccess(CURRENT_USER_ID, TARGET_USER_ID);

        // when & then
        assertThatThrownBy(
                () -> presenceService.findPresence(CURRENT_USER_ID, TARGET_USER_ID)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorEnum())
                .isEqualTo(ErrorEnum.PRESENCE_ACCESS_DENIED);

        verify(friendValidator)
                .validatePresenceAccess(CURRENT_USER_ID, TARGET_USER_ID);
        verifyNoInteractions(userValidator);
        verifyNoInteractions(redisPresenceService);
    }

    @Test
    @DisplayName("자기 자신의 접속 상태를 조회할 때 비활성 사용자라면 예외가 발생한다")
    void findPresence_자기_자신이_비활성_사용자라면_예외가_발생한다() {
        // given
        doThrow(new RuntimeException("비활성 사용자입니다."))
                .when(userValidator)
                .validateActiveUser(CURRENT_USER_ID);

        // when & then
        assertThatThrownBy(
                () -> presenceService.findPresence(CURRENT_USER_ID, CURRENT_USER_ID)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("비활성 사용자입니다.");

        verify(userValidator).validateActiveUser(CURRENT_USER_ID);
        verifyNoInteractions(friendValidator);
        verifyNoInteractions(redisPresenceService);
    }

    @Test
    @DisplayName("친구가 비활성 사용자라면 접속 상태를 조회할 수 없다")
    void findPresence_친구가_비활성_사용자라면_예외가_발생한다() {
        // given
        doThrow(new RuntimeException("비활성 사용자입니다."))
                .when(userValidator)
                .validateActiveUser(TARGET_USER_ID);

        // when & then
        assertThatThrownBy(
                () -> presenceService.findPresence(CURRENT_USER_ID, TARGET_USER_ID)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("비활성 사용자입니다.");

        verify(friendValidator)
                .validatePresenceAccess(CURRENT_USER_ID, TARGET_USER_ID);
        verify(userValidator).validateActiveUser(TARGET_USER_ID);
        verifyNoInteractions(redisPresenceService);
    }

    @Test
    @DisplayName("세션이 존재하지 않으면 오프라인 상태를 반환한다")
    void findPresence_세션이_없으면_오프라인_상태를_반환한다() {
        // given
        when(redisPresenceService.getSessionCount(TARGET_USER_ID))
                .thenReturn(0);

        // when
        UserPresenceResponse response =
                presenceService.findPresence(CURRENT_USER_ID, TARGET_USER_ID);

        // then
        assertThat(response.userId()).isEqualTo(TARGET_USER_ID);
        assertThat(response.online()).isFalse();
        assertThat(response.activeSessions()).isEqualTo(0);

        verify(friendValidator)
                .validatePresenceAccess(CURRENT_USER_ID, TARGET_USER_ID);
        verify(userValidator).validateActiveUser(TARGET_USER_ID);
        verify(redisPresenceService).getSessionCount(TARGET_USER_ID);
    }

    @Test
    @DisplayName("여러 세션이 존재하면 온라인 상태와 세션 수를 반환한다")
    void findPresence_여러_세션이_존재하면_온라인_상태와_세션_수를_반환한다() {
        // given
        when(redisPresenceService.getSessionCount(TARGET_USER_ID))
                .thenReturn(3);

        // when
        UserPresenceResponse response =
                presenceService.findPresence(CURRENT_USER_ID, TARGET_USER_ID);

        // then
        assertThat(response.userId()).isEqualTo(TARGET_USER_ID);
        assertThat(response.online()).isTrue();
        assertThat(response.activeSessions()).isEqualTo(3);

        verify(friendValidator)
                .validatePresenceAccess(CURRENT_USER_ID, TARGET_USER_ID);
        verify(userValidator).validateActiveUser(TARGET_USER_ID);
        verify(redisPresenceService).getSessionCount(TARGET_USER_ID);
    }
}