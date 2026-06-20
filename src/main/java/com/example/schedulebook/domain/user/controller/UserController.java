package com.example.schedulebook.domain.user.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.user.dto.request.UpdateUserPasswordRequest;
import com.example.schedulebook.domain.user.dto.request.UpdateUserRequest;
import com.example.schedulebook.domain.user.dto.response.UpdateUserResponse;
import com.example.schedulebook.domain.user.dto.response.UserResponse;
import com.example.schedulebook.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS, userService.findMyProfile(SecurityUtils.getCurrentUserId()))
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UpdateUserResponse>> updateMyProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS, userService.updateMyProfile(request, SecurityUtils.getCurrentUserId())
                )
        );
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updateMyPassword(@Valid @RequestBody UpdateUserPasswordRequest request) {
        userService.updateMyPassword(request, SecurityUtils.getCurrentUserId());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null)
        );
    }
}
