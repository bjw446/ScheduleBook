package com.example.schedulebook.domain.auth.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.example.schedulebook.domain.auth.dto.request.SignupRequest;
import com.example.schedulebook.domain.auth.dto.response.LoginResponse;
import com.example.schedulebook.domain.auth.dto.response.SignupResponse;
import com.example.schedulebook.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(SuccessEnum.REGISTER_SUCCESS, authService.signup(request))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(SuccessEnum.LOGIN_SUCCESS, authService.login(request))
        );
    }
}
