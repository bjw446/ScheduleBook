package com.example.schedulebook.domain.auth.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.security.JwtProvider;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.example.schedulebook.domain.auth.dto.request.SignupRequest;
import com.example.schedulebook.domain.auth.dto.response.LoginResponse;
import com.example.schedulebook.domain.auth.dto.response.SignupResponse;
import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserStatus;
import com.example.schedulebook.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public SignupResponse signup(SignupRequest request) {
        validateDuplicateUser(request);

        User user = User.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.email(),
                request.phoneNumber()
        );

        User savedUser = userRepository.save(user);

        return SignupResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId()).orElseThrow(
                () -> new BaseException(ErrorEnum.LOGIN_FAILED)
        );

        validateUserStatus(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        try {
            user.login();

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BaseException(ErrorEnum.LOGIN_CONFLICT);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId());

        return LoginResponse.from(user, accessToken);
    }

    private void validateDuplicateUser(SignupRequest request) {
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

    private void validateUserStatus(User user) {
        if (user.getUserStatus() == UserStatus.WITHDRAW) {
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        if (user.getUserStatus() == UserStatus.DENIED) {
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }
    }
}
