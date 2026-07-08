package com.example.schedulebook.domain.user.service;

import com.example.schedulebook.domain.user.dto.request.UpdateUserPasswordRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.dto.response.UpdateUserResponse;
import com.example.schedulebook.domain.user.dto.response.UserResponse;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;

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

        userValidator.validatePassword(request, user);

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }
}
