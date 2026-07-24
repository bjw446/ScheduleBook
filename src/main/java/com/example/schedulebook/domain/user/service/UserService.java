package com.example.schedulebook.domain.user.service;

import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.event.OutboxSaveEvent;
import com.example.schedulebook.domain.user.event.UserWithdrawEvent;
import com.example.schedulebook.domain.user.dto.request.UpdateUserPasswordRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.dto.request.WithdrawUserRequest;
import com.example.schedulebook.domain.user.dto.response.UpdateUserResponse;
import com.example.schedulebook.domain.user.dto.response.UserResponse;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.repository.UserRepository;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public UserResponse findMyProfile(Long currentUserId) {
        User user = userValidator.validateActiveUser(currentUserId);

        return UserResponse.from(user);
    }

    public UpdateUserResponse updateMyProfile(UpdateUserRequest request, Long currentUserId) {
        User user = userValidator.validateActiveUser(currentUserId);

        userValidator.validateDuplicate(request, user);

        user.updateProfile(request.nickname(), request.email(), request.phoneNumber());

        return UpdateUserResponse.from(user);
    }

    public void updateMyPassword(UpdateUserPasswordRequest request, Long currentUserId) {
        User user = userValidator.validateActiveUser(currentUserId);

        userValidator.validatePassword(request.currentPassword(), user);

        userValidator.validateNewPassword(request.newPassword(), user);

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    public void withdraw(WithdrawUserRequest request, Long currentUserId) {
        User user = userValidator.validateActiveUser(currentUserId);

        userValidator.validatePassword(request.password(), user);

        String loginId = user.getLoginId();

        user.withdraw(passwordEncoder.encode(request.password()));

        userRepository.saveAndFlush(user);

        UserWithdrawEvent userWithdrawEvent = new UserWithdrawEvent(user.getId(), loginId);

        applicationEventPublisher.publishEvent(new OutboxSaveEvent(
                OutboxAggregateType.USER,
                user.getId(),
                OutboxEventType.USER_WITHDRAW,
                userWithdrawEvent
        ));
    }
}
