package com.example.schedulebook.domain.user.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.user.dto.request.UpdateUserPasswordRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.dto.response.UpdateUserResponse;
import com.example.schedulebook.domain.user.dto.response.UserResponse;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse findMyProfile(Long currentUserId) {
        User user = validateUser(currentUserId);

        return UserResponse.from(user);
    }

    public UpdateUserResponse updateMyProfile(UpdateUserRequest request, Long currentUserId) {
        User user = validateUser(currentUserId);

        validateDuplicate(request, user);

        user.updateProfile(request.nickname(), request.email(), request.phoneNumber());

        return UpdateUserResponse.from(user);
    }

    public void updateMyPassword(UpdateUserPasswordRequest request, Long currentUserId) {
        User user = validateUser(currentUserId);

        validatePassword(request, user);

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    private User validateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        return user;
    }

    private void validateDuplicate(UpdateUserRequest request, User user) {
        if (!user.getNickname().equals(request.nickname()) && userRepository.existsByNickname(request.nickname())) {
            throw new BaseException(ErrorEnum.NICKNAME_ALREADY_EXISTS);
        }

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS);
        }

        if (!user.getPhoneNumber().equals(request.phoneNumber()) && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BaseException(ErrorEnum.PHONE_NUMBER_ALREADY_EXISTS);
        }
    }

    private void validatePassword(UpdateUserPasswordRequest request, User user) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_NOT_MATCH);
        }

        if (!passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_SAME_AS_OLD);
        }
    }
}
