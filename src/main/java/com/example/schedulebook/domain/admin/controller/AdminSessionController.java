package com.example.schedulebook.domain.admin.controller;

import com.example.schedulebook.common.enums.SuccessEnum;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.SecurityUtils;
import com.example.schedulebook.domain.admin.service.AdminSessionService;
import com.example.schedulebook.domain.auth.dto.response.SessionInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminSessionController {
    private final AdminSessionService adminSessionService;

    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<ApiResponse<List<SessionInfoResponse>>> getAllUserSessions(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        adminSessionService.findAllUserSessions(SecurityUtils.getCurrentUserId(), userId)
                )
        );
    }

    @DeleteMapping("/users/{userId}/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> logoutUserOneSession(@PathVariable Long userId, @PathVariable String sessionId) {
        adminSessionService.logoutUserOneSession(SecurityUtils.getCurrentUserId(), userId, sessionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.LOGOUT_SUCCESS,
                        null
                )
        );
    }

    @DeleteMapping("/users/{userId}/sessions")
    public ResponseEntity<ApiResponse<Void>> logoutUserAllSessions(@PathVariable Long userId) {
        adminSessionService.logoutUserAllSession(SecurityUtils.getCurrentUserId(), userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                        SuccessEnum.LOGOUT_SUCCESS,
                        null
                )
        );
    }
}
