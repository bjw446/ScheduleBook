package com.example.schedulebook.domain.user.validator;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.auth.dto.request.SignupRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserPasswordRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User validateActiveUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.USER_NOT_FOUND)
        );

        validateUserStatus(user);

        return user;
    }

    public void validateDuplicate(UpdateUserRequest request, User user) {
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

    public void validatePassword(UpdateUserPasswordRequest request, User user) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_NOT_MATCH);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_SAME_AS_OLD);
        }
    }

    public void validateShareMyself(Long currentUserId, Long friendId) {
        if (currentUserId.equals(friendId)) {
            throw new BaseException(ErrorEnum.CANNOT_SHARE_MYSELF);
        }
    }

    public void validateUserStatus(User user) {
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BaseException(ErrorEnum.USER_NOT_ACTIVE);
        }
    }

    public void validateDuplicateUser(SignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BaseException(ErrorEnum.LOGIN_ID_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new BaseException(ErrorEnum.NICKNAME_ALREADY_EXISTS);
        }

        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BaseException(ErrorEnum.PHONE_NUMBER_ALREADY_EXISTS);
        }
    }

    public void validateLoginUserStatus(User user) {
        if (user.getUserStatus() == UserStatus.WITHDRAW) {
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        if (user.getUserStatus() == UserStatus.DENIED) {
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }
    }
}
